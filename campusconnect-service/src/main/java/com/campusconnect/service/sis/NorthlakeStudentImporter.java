package com.campusconnect.service.sis;

import java.io.File;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.apache.log4j.Logger;

import com.campusconnect.common.CustomerContext;
import com.campusconnect.common.DateUtils;
import com.campusconnect.common.StringHelper;
import com.campusconnect.domain.Student;
import com.campusconnect.domain.sis.SisStudentFile;
import com.campusconnect.domain.sis.SisStudentRecord;
import com.campusconnect.persistence.AdvisorDao;
import com.campusconnect.persistence.StudentDao;

/**
 * LEGACY SMELL #2: COPY-PASTE DUPLICATION (twin A).
 *
 * This class and {@link RivertonStudentImporter} are roughly 80% identical.
 * The real differences are:
 *   1. RIVERTON maps a campus code that Northlake does not send,
 *    2. RIVERTON carries athleticsCode,
 *    3. RIVERTON rejects records with no advisor-staff-id; Northlake accepts them,
 *    4. the log prefix.
 * Everything else was copied with Ctrl-C in 2015 and has been patched twice in
 * one file and once in the other since.
 *
 * Both are also LEGACY SMELL #4: raw Lists, hand-rolled loops, swallowed
 * exceptions, and no streams.
 */
public class NorthlakeStudentImporter {

    private static final Logger LOG = Logger.getLogger(NorthlakeStudentImporter.class);

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
            LOG.info("NORTHLAKE import: " + records.size() + " records from " + file.getName());

            Iterator it = records.iterator();
            while (it.hasNext()) {
                SisStudentRecord record = (SisStudentRecord) it.next();
                if (StringHelper.isEmpty(record.getSisId())) {
                    LOG.warn("NORTHLAKE import: skipping record with no SIS id");
                    continue;
                }
                Student student = studentDao.findBySisId(record.getSisId());
                if (student == null) {
                    student = new Student();
                    student.setCustomerCode(CustomerContext.NORTHLAKE);
                    student.setCreatedAt(new Date());
                }
                applyCommonFields(student, record);

                // Northlake-specific: no athletics, no campus mapping.
                student.setUpdatedAt(new Date());
                if (student.getId() == null) {
                    studentDao.save(student);
                } else {
                    studentDao.update(student);
                }
                imported++;
            }
        } catch (Exception e) {
            // Swallowed. The batch wrapper reports success.
            e.printStackTrace();
        }
        LOG.info("NORTHLAKE import finished, " + imported + " records");
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
        }
        return "ACTIVE";
    }
}
