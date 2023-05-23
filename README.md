# Gas_Guardian

# 아이디어 기술 요건 파악

계획 기간: 2023년 4월 3일
진행 단계: 완료

### 🔲 아이디어 기술 요건

---

1. Stm32(Cortex - M4) 혹은 HEB MCU(Cortex-M4) 사용 → HEB MCU(Cortex-M4)로 결정
2. Raspberry Pi를 이용한 제어 및 DB 관리
3. 추가 보드 사용 → Arduino 사용

### 🔲 구현 기능(실제 결과물)

---

1. Arduino (Raspberry와 Bluetooth 통신)
    1. LCD로 타이머 및 Safe Mode On/Off, Valve On/Off, 화력 세기 확인
    2. 버튼으로 타이머 및 화력 세기 입력 후 결과 Raspberry로 전송
        - 사용한 버튼 목록
            1. 가스불 On/Off - 가스가 켜졌다는 상황 가정 // 송신 메세지 [COR]FIREON@1~8
                - 1번 버튼으로 불 세기 조절 (1~8)
                - 2번 버튼으로 불 On/Off
            2. 타이머 버튼
                - 타이머 기능 자체 On/Off
                - 분, 초, 리셋 버튼
                    
                    → 분 : 0~9분 1분씩 증가
                    
                    → 초 : 0~50초 10초씩 증가
                    
                    → 취소 : 버튼 저장값 초기화
                    
            3. 밸브 On/Off
            4. Safe Mode On/Off
2. Cortex M4 (Raspberry와 WiFi 통신)
    
    일정 가스량 사용시 DB에 결과 저장
    
    1. 서보 모터(가스 밸브) : 잠금 여부 확인
    2. LED : 화력 세기 표현
    3. 조도 센서 : 화제 여부 확인
        
        → 조도 센서 33 이상일 경우 화재로 가정, Firebase를 이용한 FCM 메세지 송신
        
    4. 적외선 센서 : Safe Mode 실행시 작동
        
        → 2000 이하로 값이 나올경우 멀어졌다고 판단, 10초 카운트 후 Valve Off 시행
        
        → 특이사항 : 멀어질수록 값이 작게 나오고 가까울수록 크게 나옴
        
3. Raspberry (Main Thread Server)
    1. Android와 WiFi 통신
    2. Arduino와 Bluetooth 통신
    3. Cortex M4와 WiFi 통신
    4. Maria DB 가스 사용량 체크
        
        → iot_gas_db라는 client를 실행시켜 Maria DB에 결과를 저장함
        
4. Android
    1. 밸브 On/Off 여부 확인 및 실제 버튼 작동 가능
    2. Arduino에서 타이머 실행하면 Android에서도 확인 가능
    3. Arduino에서 화력 세기를 설정하면 Android에서도 확인 가능
    4. 화재 발생시 FCM 수신
    5. Maria DB와 연결된 html 접속
5. Maria DB & Apache HTTP Server
    1. 가스 사용량 표 및 차트로 표현
    2. 가스 사용량을 토대로 가스비 계산 가능

---

- **🔲 구현 기능(계획)**
    1. Arduino (Raspberry와 Bluetooth)
        1. 센서값 LCD 출력
        2. 버튼으로 타이머 및 불 세기 입력 후 결과 Raspberry로 전송
            - 필요한 버튼 목록 / 총 6개
                1. 가스불 On/Off - 가스가 켜졌다는 상황 가정 // 송신 메세지 [1]FIRE@ON@1~3
                    - 1번 버튼으로 불 세기 조절 (1~3) - **1**
                    - 2번 버튼으로 불 On/Off - **1**
                2. 타이머 버튼
                    - 타이머 기능 자체 On/Off - **1**
                    - 분, 초, 리셋 버튼 - **3**
                        
                        → 분 : 0~9분 1분씩 증가
                        
                        → 초 : 0~50초 10초씩 증가
                        
                        → 취소 : 버튼 저장값 초기화
                        
                3. 벨브 ON / OFF
    2. Stm32 (Raspberry와 WiFi 통신)
        
        센서값 및 서보모터 상태는 1초에 한번씩 Raspberry로 값 송신
        
        1. 가스 센서 2종류
            - 도시가스 센서 → 가스 유출 확인
            - 일산화탄소 센서 → 화재 확인
        2. 온도 센서 → 조도 센서로 대체
            
            → 조도 센서 300 이상일 경우 화재로 가정
            
        3. **서보 모터(가스 벨브) : 잠금 여부 확인**
    3. Raspberry
        1. Android와 WiFi 통신
        2. Maria DB 가스 사용량 체크
            
            → 불 세기로 확인 (ex. 1번 불 세기 1++ / 2번 불 세기 2++)
            
    4. Android
        1. 서보 모터 On/Off 확인 후 On인경우 버튼으로 Off로 동작하게 함
        2. 가스 사용량을 토대로 가스비 대략 계산
