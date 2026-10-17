package com.meokji.api.notice.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.meokji.api.notice.service.NoticeService;

/** 공지사항.  주소: /api/notices */
@RestController
@RequestMapping("/api/notices")
public class NoticeController {

    @Autowired
    private NoticeService noticeService;

    // 목록  GET /api/notices
    @GetMapping
    public void getList() {
    }
}
