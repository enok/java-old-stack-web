package com.campusconnect.service.sis;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.apache.log4j.Logger;

import com.campusconnect.common.CustomerContext;
import com.campusconnect.common.CustomerProperties;
import com.campusconnect.common.DateUtils;
import com.campusconnect.common.StringHelper;
import com.campusconnect.domain.Student;
import com.campusconnect.domain.sis.SisStudentFile;
import com.campusconnect.domain.sis.SisStudentRecord;
import com.campusconnect.persistence.AdvisorDao;
import com.campusconnect.persistence.StudentDao;

/**
 * LEGACY SMELL #2: COPY-PASTE DUPLICATION (twin B).
 *
 * Diff against {@link NorthlakeStudentImporter}:
 *   - campus code mapping (campusCodeMap property),
 *   - athleticsCode is carried through,
 *   - records with no advisor-staff-id are REJECTED, not accepted,
 *   - the "L" status code is handled here and not in the Northlake twin,
 *     because it was added for Riverton in 2017 and never back-ported.
 *
 * The drift in translateStatus() is the whole argument for consolidating these
 * two classes before anything else in the fork track.
 */
public class RivertonStudentImporter {

    private static final Logger LOG = Logger.getLogger(RivertonStudentImporter.class);

    private StudentDao studentDao;
    private AdvisorDao advisorDao;

    public void setStudentDao(StudentDao studentDao) { this.studentDao = studentDao; }
    public void setAdvisorDao(AdvisorDao advisorDao) { this.advisorDao = advisorDao; }

    public int importFile(File file) {
        int imported = 0;
        try {
            JAXBContext context = JAXBContext.newInstance(SisStudentFile.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            SisStudentFile parsed = (SisStudentFile) unmarshaller.unmarshal(file);

            List records = parsed.getStudents();
            LOG.info("RIVERTON import: " + records.size() + " records from " + file.getName());

            Map campusMap = buildCampusMap();

            Iterator it = records.iterator();
            while (it.hasNext()) {
                SisStudentRecord record = (SisStudentRecord) it.next();
                if (StringHelper.isEmpty(record.getSisId())) {
                    LOG.warn("RIVERTON import: skipping record with no SIS id");
                    continue;
                }
                // DIFFERENT BUSINESS RULE: Riverton rejects advisor-less records.
                if (StringHelper.isEmpty(record.getAdvisorStaffId())) {
                    LOG.warn("RIVERTON import: rejecting " + record.getSisId() + ", no advisor staff id");
                    continue;
                }
                Student student = studentDao.findBySisId(record.getSisId());
                if (student == null) {
                    student = new Student();
                    student.setCustomerCode(CustomerContext.RIVERTON);
                    student.setCreatedAt(new Date());
                }
                applyCommonFields(student, record);

                // Riverton-specific fields.
                student.setAthleticsCode(record.getAthleticsCode());
                // RIVERTON BRANCH ONLY: NCAA eligibility term is appended to the
                // program code because there is nowhere else to put it. This field
                // exists on no other branch and blocks a straight merge.
                if (!StringHelper.isEmpty(record.getAthleticsCode())) {
                    student.setProgramCode(student.getProgramCode() + "/NCAA-"
                            + CustomerProperties.get("term.current"));
                }
                if (!StringHelper.isEmpty(record.getCampusCode())) {
                    Object mapped = campusMap.get(StringHelper.safeUpper(record.getCampusCode()));
                    if (mapped == null) {
                        LOG.warn("RIVERTON import: unmapped campus code " + record.getCampusCode());
                    } else {
                        student.setProgramCode(student.getProgramCode() + "-" + mapped);
                    }
                }

                student.setUpdatedAt(new Date());
                if (student.getId() == null) {
                    studentDao.save(student);
                } else {
                    studentDao.update(student);
                }
                imported++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        LOG.info("RIVERTON import finished, " + imported + " records");
        return imported;
    }

    private void applyCommonFields(Student student, SisStudentRecord record) {
        student.setSisId(record.getSisId());
        student.setFirstName(record.getFirstName());
        student.setLastName(record.getLastName());
        student.setEmail(record.getEmail());
        student.setPhone(record.getPhone());
        student.setProgramCode(record.getProgramCode());
        student.setBirthDate(DateUtils.parseSis(record.getBirthDate()));
        student.setEnrollmentStatus(translateStatus(record.getEnrollmentStatus()));
        if (!StringHelper.isEmpty(record.getGpa())) {
            try {
                student.setGpa(new Double(Double.parseDouble(record.getGpa())));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (!StringHelper.isEmpty(record.getCreditsCompleted())) {
            try {
                student.setCreditsCompleted(new Integer(Integer.parseInt(record.getCreditsCompleted())));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (!StringHelper.isEmpty(record.getAdvisorStaffId())) {
            student.setAdvisor(advisorDao.findByStaffId(record.getAdvisorStaffId()));
        }
    }

    private String translateStatus(String sisStatus) {
        if (StringHelper.isEmpty(sisStatus)) {
            return "ACTIVE";
        }
        String upper = StringHelper.safeUpper(sisStatus);
        if ("E".equals(upper)) {
            return "ACTIVE";
        } else if ("P".equals(upper)) {
            return "PROBATION";
        } else if ("W".equals(upper)) {
            return "WITHDRAWN";
        } else if ("G".equals(upper)) {
            return "GRADUATED";
        } else if ("L".equals(upper)) {
            // Only in this twin. Northlake maps "L" to ACTIVE by accident.
            return "LEAVE";
        }
        return "ACTIVE";
    }

    private Map buildCampusMap() {
        Map out = new HashMap();
        String raw = CustomerProperties.get(CustomerContext.RIVERTON, "sis.campusCodeMap");
        if (StringHelper.isEmpty(raw)) {
            return out;
        }
        String[] pairs = raw.split(",");
        for (int i = 0; i < pairs.length; i++) {
            String[] kv = pairs[i].split(":");
            if (kv.length == 2) {
                out.put(kv[0].trim().toUpperCase(), kv[1].trim());
            }
        }
        return out;
    }
}
