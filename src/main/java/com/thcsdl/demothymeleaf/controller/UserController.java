package com.thcsdl.demothymeleaf.controller;

import com.thcsdl.demothymeleaf.dto.request.UserBookingCreateRequest;
import com.thcsdl.demothymeleaf.entity.Booking;
import com.thcsdl.demothymeleaf.entity.Member;
import com.thcsdl.demothymeleaf.entity.Room;
import com.thcsdl.demothymeleaf.repository.BookingRepository;
import com.thcsdl.demothymeleaf.repository.MemberRepository;
import com.thcsdl.demothymeleaf.service.BookingService;
import jakarta.servlet.http.HttpSession;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDate;

@RequestMapping("/user")
@Controller
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserController {
    PasswordEncoder passwordEncoder;
    BookingService bookingService;
    private final BookingRepository bookingRepository;
    private final MemberRepository memberRepository;

    @GetMapping("/create")
    public String createBooking(Model model, HttpSession session) {
        Member member = (Member) session.getAttribute("member");
        if ( member == null ) {
            return "redirect:/loginOrRegister";
        }

        model.addAttribute("createBooking", new UserBookingCreateRequest());
        model.addAttribute("createFail", 0);
        return "userCreateBooking";
    }

    @PostMapping("/create")
    public String createBooking(@ModelAttribute("createBooking") UserBookingCreateRequest request, HttpSession session, Model model) {
       Room room = bookingService.findAvailableRoom(request.getRoomType(),request.getBookedDate(),request.getBookedTime(),request.getExpiredTime());
       if (room == null) {
           model.addAttribute("createFail", 3);
           return "userCreateBooking";
       }
       else
       {
           model.addAttribute("createFail", 2);
           Booking booking = new Booking();
           Member member = (Member) session.getAttribute("member");
           booking.setRoomid(room);
           booking.setBookedDate(request.getBookedDate());
           booking.setBookedTime(request.getBookedTime());
           booking.setExpiredTime(request.getExpiredTime());
           booking.setMemberid(member);
           booking.setDatetimeOfBooking(LocalDate.now());
           booking.setPaymentStatus("Unpaid");
           Member member1 = memberRepository.findById(member.getId()).orElseThrow(RuntimeException::new);
           Double paymentDue = booking.getRoomid().getPrice()/60 * Duration.between(booking.getBookedTime(),booking.getExpiredTime()).toMinutes();

           if (member1.getBookingList().isEmpty()){
               paymentDue = paymentDue*0.8;
           }

           if (member1.getRank() == null)
           {}
           else if (member1.getRank().equals("Silver")) {
               paymentDue = paymentDue*0.95;
           }
           else if (member1.getRank().equals("Gold")) {
               paymentDue = paymentDue*0.92;
           }
           else if (member1.getRank().equals("Diamond")) {
               paymentDue = paymentDue*0.88;
           }

           Long minutes = Duration.between(booking.getBookedTime(),booking.getExpiredTime()).toMinutes();
            if (minutes < 240) {}
            else if (minutes < 360){
                paymentDue = paymentDue*0.93;
            }
            else {
                paymentDue = paymentDue*0.9;
            }
            booking.setPaymentDue(paymentDue);
           member1.setPaymentDue(member1.getPaymentDue()+paymentDue);
           memberRepository.save(member1);
           bookingRepository.save(booking);
           session.setAttribute("member", member1);
           return "userCreateBooking";
       }
    }

    @GetMapping("/service")
    public String service(Model model, HttpSession session) {
        Member member = (Member) session.getAttribute("member");
        if ( member != null ) {
            model.addAttribute("member", member);
        }
        return "service";
    }

    @PostMapping("/update")
    public String update(HttpSession session, @ModelAttribute("updateMember") Member member, Model model) {

        Member member1 = (Member) session.getAttribute("member");
        if (!member.getUsername().isEmpty()){
            member1.setUsername(member.getUsername());
        }
        if (!member.getPassword().isEmpty()){
            member1.setPassword(passwordEncoder.encode(member.getPassword()));
        }
        if (!member.getEmail().isEmpty()){
            if (memberRepository.findByEmail(member.getEmail()) != null) {
                throw new RuntimeException();
            }
            member1.setEmail(member.getEmail());
        }
        memberRepository.save(member1);
        session.setAttribute("member", member1);
        return "redirect:/myInfo";
    }

    @PostMapping("/cancel")
    public String cancelBooking(@RequestParam(name = "id") Integer id, HttpSession session) {
        Booking cancelBooking = bookingRepository.findById(id).orElseThrow(RuntimeException::new);
        Member member = (Member) session.getAttribute("member");
        member.setPenaltyExemption(member.getPenaltyExemption()-1);
        if (member.getPenaltyExemption()==0) {
            if (member.getRank() == null || member.getRank().equals("Silver")) {
                member.setPenaltyExemption(3);
            }
            else if (member.getRank().equals("Gold") || member.getRank().equals("Diamond")) {
                member.setPenaltyExemption(4);
            }
            member.setPaymentDue(member.getPaymentDue()+200);
        }
        member.setPaymentDue(member.getPaymentDue()-cancelBooking.getPaymentDue());
        memberRepository.save(member);
        cancelBooking.setPaymentStatus("Cancelled");
        bookingRepository.save(cancelBooking);
        Member member1 =memberRepository.findByEmail(member.getEmail());
        session.setAttribute("member", member1);
        return "redirect:/myInfo";
    }
}
