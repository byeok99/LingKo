-- 표시 언어와 모국어는 저장만 되고 읽는 코드가 없어 설정 기능 전체와 함께 제거한다.
ALTER TABLE users DROP COLUMN display_language;
ALTER TABLE users DROP COLUMN native_language;
