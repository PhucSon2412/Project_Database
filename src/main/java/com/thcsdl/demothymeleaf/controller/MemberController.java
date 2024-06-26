package com.thcsdl.demothymeleaf.controller;

import com.thcsdl.demothymeleaf.entity.Booking;
import com.thcsdl.demothymeleaf.entity.Member;
import com.thcsdl.demothymeleaf.repository.BookingRepository;
import com.thcsdl.demothymeleaf.repository.MemberRepository;
import com.thcsdl.demothymeleaf.service.MemberService;
import jakarta.servlet.http.HttpSession;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/admin/members")
@Controller
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MemberController {
    MemberService memberService;
    PasswordEncoder passwordEncoder;
    private final BookingRepository bookingRepository;
    private final MemberRepository memberRepository;

    @GetMapping
    public String getAllMembers(Model model, HttpSession session) {

        Member member = (Member) session.getAttribute("member");
        if ( member == null ) {
            return "redirect:/loginOrRegister";
        }
        if ( !member.getRole().equals("ADMIN"))
            return "redirect:/";

        List<Member> members = memberService.getAllMembers();
        model.addAttribute("members", members);
        return "members";
    }

    @GetMapping("/search")
    public String searchMemberByEmail(@RequestParam(name = "email") String email, Model model, HttpSession session) {
        Member member1 = (Member) session.getAttribute("member");
        if ( member1 == null ) {
            return "redirect:/loginOrRegister";
        }
        if ( !member1.getRole().equals("ADMIN"))
            return "redirect:/";

        Member member = memberService.getMemberByEmail(email);
        if (email.isEmpty()) {
            model.addAttribute("members", memberService.getAllMembers());
        }
        else model.addAttribute("members", member);

        return "members";
    }

    @PostMapping("/update")
    public String updateMember(@RequestParam(name = "id") String id, Model model, HttpSession session) {
        Member member = memberService.getMemberById(id);
        model.addAttribute("updateMember", member);
        session.setAttribute("updateMember", member);

        List<Booking> notpaid = bookingRepository.findAll().stream().filter(booking -> booking.getMemberid().toString2().equals(member.toString2())).toList().stream().filter(booking -> booking.getPaymentStatus().equals("Unpaid")).toList();
        Double paymentDue = member.getPaymentDue();
        for (Booking booking : notpaid) {
            paymentDue = paymentDue - booking.getPaymentDue();
        }
        model.addAttribute("paymentDue", paymentDue);

        return "updateMember";
    }

    @PostMapping("/realupdate")
    public String realUpdateMember(@ModelAttribute("updateMember") Member member, HttpSession session) {
        Member member1 = (Member) session.getAttribute("updateMember");
        if (!member.getUsername().isEmpty()){
            memberService.updateMemberUsername(member1.getId(), member.getUsername());
        }
        if (!member.getPassword().isEmpty()){
            memberService.updateMemberPassword(member1.getId(), passwordEncoder.encode(member.getPassword()));
        }
        if (!member.getEmail().isEmpty()){
            if (memberService.getMemberByEmail(member.getEmail()) != null) {
                if (!memberService.getMemberByEmail(member.getEmail()).getId().equals(member1.getId())) {
                    throw new RuntimeException();
                }
            }
            memberService.updateMemberEmail(member1.getId(), member.getEmail());
        }
        return "redirect:/admin/members";
    }


    @PostMapping("/delete")
    public String deleteMember(@RequestParam(name = "id") String id) {
        memberService.deleteMember(id);
        return "redirect:/admin/members";
    }
}
