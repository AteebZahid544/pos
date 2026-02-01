//package com.example.pos.Controller;
//
//import com.example.pos.entity.pos.AttendanceRecord;
//import com.example.pos.repo.pos.AttendanceRepository;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PathVariable;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//import java.util.List;
//import java.util.stream.Collectors;
//
//@RestController
//@RequestMapping("/attendance")
//public class AttendanceController {
//
//    private final AttendanceRepository attendanceRepository;
//
//    public AttendanceController(AttendanceRepository attendanceRepository) {
//        this.attendanceRepository = attendanceRepository;
//    }
//
//    @GetMapping("/all")
//    public List<AttendanceRecord> getAllAttendance() {
//        return attendanceRepository.findAll();
//    }
//
//    @GetMapping("/employee/{id}")
//    public List<AttendanceRecord> getAttendanceByEmployee(@PathVariable Long id) {
//        return attendanceRepository.findAll().stream()
//                .filter(r -> r.getEmployeeId().equals(id))
//                .collect(Collectors.toList());
//    }
//}
