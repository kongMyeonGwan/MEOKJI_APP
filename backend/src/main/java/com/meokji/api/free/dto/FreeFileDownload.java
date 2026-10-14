package com.meokji.api.free.dto;

/**
 * 사진 내려받기에 필요한 것 (서비스 → 컨트롤러). JSON 으로 나가지 않는다.
 *
 * 컨트롤러는 이 셋으로 응답을 만든다 — bytes 는 본문, ext 는 Content-Type,
 * name 은 Content-Disposition 의 파일명(내려받을 때 UUID 대신 이 이름으로 저장된다).
 */
public record FreeFileDownload(String name, String ext, byte[] bytes) {
}
