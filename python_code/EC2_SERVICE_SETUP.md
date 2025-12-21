# Python FastAPI 서버 항상 실행 설정

EC2에서 Python FastAPI 서버를 systemd 서비스로 등록하여 항상 실행되도록 설정하는 방법입니다.

## 1. systemd 서비스 파일 생성

```bash
sudo nano /etc/systemd/system/saintplus-python.service
```

다음 내용을 입력하세요:

```ini
[Unit]
Description=Saint+ Python Recommendation Server
After=network.target

[Service]
Type=simple
User=ubuntu
WorkingDirectory=/home/ubuntu/saintplus-python-server
Environment="PATH=/home/ubuntu/saintplus-python-server/venv/bin"
ExecStart=/home/ubuntu/saintplus-python-server/venv/bin/uvicorn main:app --host 0.0.0.0 --port 8000
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

**⚠️ 주의사항:**
- `User`: 실제 사용자명으로 변경 (기본값: ubuntu)
- `WorkingDirectory`: Python 서버가 있는 실제 경로로 변경
- `ExecStart`: venv 경로를 실제 환경에 맞게 변경

## 2. 서비스 활성화 및 시작

```bash
# 서비스 파일 리로드
sudo systemctl daemon-reload

# 서비스 시작
sudo systemctl start saintplus-python

# 부팅 시 자동 시작 설정
sudo systemctl enable saintplus-python

# 상태 확인
sudo systemctl status saintplus-python
```

## 3. 서비스 관리 명령어

### 기본 명령어
```bash
# 서비스 시작
sudo systemctl start saintplus-python

# 서비스 중지
sudo systemctl stop saintplus-python

# 서비스 재시작
sudo systemctl restart saintplus-python

# 상태 확인
sudo systemctl status saintplus-python
```

### 로그 확인
```bash
# 실시간 로그 보기 (tail -f 같은 기능)
sudo journalctl -u saintplus-python -f

# 최근 50줄 보기
sudo journalctl -u saintplus-python -n 50

# 오늘 날짜 로그만 보기
sudo journalctl -u saintplus-python --since today
```

### 자동 시작 설정
```bash
# 부팅 시 자동 시작 활성화
sudo systemctl enable saintplus-python

# 부팅 시 자동 시작 비활성화
sudo systemctl disable saintplus-python
```

## 4. 설정 완료 후 확인사항

### ✅ 정상 작동 확인
```bash
# 1. 서비스 상태 확인 (Active: active (running) 이어야 함)
sudo systemctl status saintplus-python

# 2. 포트 확인 (8000번 포트가 LISTEN 상태여야 함)
sudo netstat -tulpn | grep 8000

# 3. API 테스트
curl http://localhost:8000
```

### 🔄 자동 재시작 테스트
```bash
# 프로세스 강제 종료 후 자동 재시작 확인
sudo pkill -f uvicorn

# 10초 후 다시 상태 확인
sleep 10
sudo systemctl status saintplus-python
```

## 5. 장점

- ✅ **EC2 재부팅 시 자동 시작**: 인스턴스 재시작 후에도 서비스가 자동으로 실행됩니다
- ✅ **크래시 시 자동 재시작**: 서버가 예기치 않게 중단되면 10초 후 자동으로 재시작됩니다
- ✅ **로그 관리**: systemd의 journalctl로 로그를 쉽게 확인할 수 있습니다
- ✅ **표준 서비스 관리**: systemctl 명령어로 일관되게 관리 가능합니다

## 6. 문제 해결

### 서비스가 시작되지 않을 때
```bash
# 1. 로그 확인
sudo journalctl -u saintplus-python -n 100

# 2. 경로 확인
ls -la /home/ubuntu/saintplus-python-server
ls -la /home/ubuntu/saintplus-python-server/venv/bin/uvicorn

# 3. 권한 확인
sudo chown -R ubuntu:ubuntu /home/ubuntu/saintplus-python-server

# 4. 서비스 파일 문법 확인
sudo systemd-analyze verify /etc/systemd/system/saintplus-python.service
```

### 서비스 파일 수정 후
```bash
# 반드시 daemon-reload 실행
sudo systemctl daemon-reload

# 서비스 재시작
sudo systemctl restart saintplus-python
```

## 7. 서버 정보

- **EC2 IP**: 3.39.70.109
- **포트**: 8000
- **엔드포인트**: POST /recommend
- **요청 형식**: 
  ```json
  {
    "prompt": "데이터 분석",
    "target": "CSE",
    "threshold": 0.1
  }
  ```
