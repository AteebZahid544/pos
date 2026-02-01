//package com.example.pos.Service;
//
//import com.example.pos.entity.pos.AttendanceRecord;
//import com.example.pos.repo.pos.AttendanceRepository;
//import com.example.pos.repo.pos.EmployeeRepository;
//import org.springframework.stereotype.Service;
//
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//
//@Service
//public class FingerprintAttendanceService {
//
//    private final AttendanceRepository attendanceRepository;
//    private final EmployeeRepository employeeRepository;
//
//    public FingerprintAttendanceService(AttendanceRepository attendanceRepository,
//                                        EmployeeRepository employeeRepository) {
//        this.attendanceRepository = attendanceRepository;
//        this.employeeRepository = employeeRepository;
//    }
//
//    // Initialize the device
//    public void initDevice() {
//        FingerprintDevice device = new FingerprintDevice("192.168.1.100"); // IP of machine
//        device.onScan(this::handleScan); // Callback on scan
//        device.connect();
//    }
//
//    // Handle scanned fingerprint
//    private void handleScan(FingerprintData data) {
//        Long employeeId = employeeRepository.findByFingerprintTemplate(data.getTemplate());
//        if(employeeId == null) return; // unknown fingerprint
//
//        LocalDate today = LocalDate.now();
//        AttendanceRecord record = attendanceRepository.findByEmployeeIdAndDate(employeeId, today);
//
//        if(record == null) {
//            // First scan → check-in
//            record = new AttendanceRecord(employeeId, LocalDateTime.now(), null, today);
//        } else if(record.getCheckOutTime() == null) {
//            // Second scan → check-out
//            record.setCheckOutTime(LocalDateTime.now());
//        }
//
//        attendanceRepository.save(record);
//    }
//}
