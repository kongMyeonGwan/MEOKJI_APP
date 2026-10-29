package com.meokji.api.catalog.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.meokji.api.catalog.service.CatalogService;

import tools.jackson.databind.JsonNode;

/** 화면이 쓰는 고정 목록. */
@RestController
@RequestMapping("/api/catalog")
public class CatalogController {

    @Autowired
    private CatalogService catalogService;

    /** 대시보드가 처음 뜰 때 필요한 목록을 한 번에. */
    @GetMapping
    public Map<String, JsonNode> read() {
        return catalogService.getSections();
    }

    /** 회원가입 · 정보수정의 주소 드롭다운이 쓰는 목록. */
    @GetMapping("/regions")
    public List<Map<String, Object>> regions() {
        return catalogService.getRegions();
    }

    /**
     * 고른 시·군·구에 딸린 읍·면·동 목록.
     *
     * 전국 동을 한 번에 내려주면 400KB 가 넘어서, 시·군·구를 고른 뒤에 따로 받아간다.
     */
    @GetMapping("/regions/dong")
    public List<String> dongs(@RequestParam String sido, @RequestParam String sigungu) {
        return catalogService.getDongs(sido, sigungu);
    }

    /** 브라우저가 잡은 내 위치에서 가장 가까운 읍·면·동. 주소 드롭다운을 채울 때 쓴다. */
    @GetMapping("/regions/nearest")
    public Map<String, String> nearestRegion(@RequestParam double lat, @RequestParam double lon) {
        return catalogService.getNearestRegion(lat, lon);
    }
}
