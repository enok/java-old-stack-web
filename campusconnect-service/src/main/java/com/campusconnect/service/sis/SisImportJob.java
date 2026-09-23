package com.campusconnect.service.sis;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Vector;

import org.apache.log4j.Logger;

import com.campusconnect.common.CustomerContext;
import com.campusconnect.common.CustomerProperties;
import com.campusconnect.service.StudentService;

/**
 * Nightly batch entry point, invoked by cron on each customer box.
 *
 * LEGACY SMELL #1, occurrence 5 of 6 (module: campusconnect-service, batch side).
 * LEGACY SMELL #4: Vector, raw types, printStackTrace.
 * LEGACY SMELL #7: no transaction anywhere in this path.
 */
public class SisImportJob {

    private static final Logger LOG = Logger.getLogger(SisImportJob.class);

    private NorthlakeStudentImporter northlakeImporter;
    private RivertonStudentImporter rivertonImporter;
    private StudentService studentService;

    public void setNorthlakeImporter(NorthlakeStudentImporter i) { this.northlakeImporter = i; }
    public void setRivertonImporter(RivertonStudentImporter i) { this.rivertonImporter = i; }
    public void setStudentService(StudentService studentService) { this.studentService = studentService; }

    public int run(String customerCode, String dropDirectory) {
        CustomerContext.set(customerCode);
        int total = 0;
        try {
            Vector files = listDropFiles(dropDirectory);
            for (int i = 0; i < files.size(); i++) {
                File file = (File) files.get(i);

                if ("RIVERTON".equals(customerCode)) {
                    total = total + rivertonImporter.importFile(file);
                } else if ("NORTHLAKE".equals(customerCode)) {
                    total = total + northlakeImporter.importFile(file);
                } else if ("SUMMIT".equals(customerCode)) {
                    // XXX CC-1301: Summit has no importer of its own, so it borrows
                    // Northlake's. Summit's home-campus field is therefore silently
                    // dropped every night and re-keyed by hand the next morning.
                    total = total + northlakeImporter.importFile(file);
                } else {
                    LOG.error("No importer configured for customer " + customerCode);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // TODO CC-0912: if listDropFiles() throws before this point on a pooled
            // thread, the next customer runs with the previous customer's context.
            CustomerContext.clear();
        }
        LOG.info("SIS import for " + customerCode + " imported " + total + " records");
        return total;
    }

    private Vector listDropFiles(String dropDirectory) {
        Vector out = new Vector();
        File dir = new File(dropDirectory);
        if (!dir.isDirectory()) {
            LOG.error("Drop directory does not exist: " + dropDirectory);
            return out;
        }
        final String pattern = CustomerProperties.get("sis.fileNamePattern");
        File[] found = dir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                if (pattern == null) {
                    return name.endsWith(".xml");
                }
                // Glob matching by hand, because "it was only one wildcard".
                String prefix = pattern.substring(0, pattern.indexOf('*'));
                String suffix = pattern.substring(pattern.indexOf('*') + 1);
                return name.startsWith(prefix) && name.endsWith(suffix);
            }
        });
        if (found == null) {
            return out;
        }
        for (int i = 0; i < found.length; i++) {
            out.add(found[i]);
        }
        return out;
    }
}
