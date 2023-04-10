#include <Keypad.h>
#include <LiquidCrystal_I2C.h>
#include <SoftwareSerial.h>
#include <Wire.h>
#include <string.h>
#include <MsTimer2.h>

int RX = 12;
int TX = 13;
SoftwareSerial BTSerial(RX, TX);

LiquidCrystal_I2C lcd(0x27, 16, 2);
const byte ROWS = 3;  // 행(rows) 개수
const byte COLS = 4;  // 열(columns) 개수
char keys[ROWS][COLS] = {
  { 'H', 'M', 'S', 'A' },
  { 'h', 'm', 's', 'B' },
  { '0', '1', '2', '3' }
};

byte rowPins[ROWS] = { 6, 7, 8 };
byte colPins[COLS] = { 5, 4, 3, 2 };
Keypad keypad = Keypad(makeKeymap(keys), rowPins, colPins, ROWS, COLS);

long int clock;
int clock_H, clock_M, clock_S;

char s1[3][25];
int j = 0;
int i = 0;
boolean start = false;
boolean end = false;

int f_set = 0;
boolean VALVE = false;
boolean onestop = false;
boolean WARN = false;
boolean SAFE = false;

void LCD(long int clock, int f_set, boolean VALVE) {
  if (WARN) {
    lcd.setCursor(0, 0);
    lcd.print("    !!FIRE!!");
    lcd.setCursor(0, 1);
    lcd.print("  !!WARNING!!");
  } else {
    clock_S = clock % 60;
    clock_M = (clock / 60) % 60;
    clock_H = (clock / 3600);

    lcd.setCursor(0, 0);
    lcd.print("TIMER :");

    lcd.setCursor(7, 0);
    lcd.print(clock_H);

    lcd.setCursor(9, 0);
    lcd.print(":");

    lcd.setCursor(10, 0);
    lcd.print(clock_M);

    lcd.setCursor(12, 0);
    lcd.print(":");

    lcd.setCursor(13, 0);
    lcd.print(clock_S);

    lcd.setCursor(15, 0);
    if (SAFE) lcd.print("*");
    else if (!SAFE) lcd.print(" ");

    lcd.setCursor(0, 1);
    lcd.print("FIRE:");

    lcd.setCursor(5, 1);
    lcd.print(f_set);

    lcd.setCursor(7, 1);
    lcd.print("VALVE:");

    lcd.setCursor(13, 1);
    if (VALVE) lcd.print("ON");
    else lcd.print("OFF");
  }
}
void READ() {
  if (BTSerial.available()) {
    char c = BTSerial.read();
    if (start) s1[j][i++] = c;
    if (c == ']') start = true;
    else if (c == '@') {
      for (--i; i < 25; i++) s1[j][i] = '\0';
      j++;
      i = 0;
    } else if (c == '\n') {
      end = true;
      for (++j; j < 3; j++) {
        for (--i; i < 25; i++) {
          s1[j][i] = '\0';
        }
        i = 0;
      }
    }
    if (end) {
      start = false;
      end = false;
      Serial.print("READ MSG : ");
      Serial.print(s1[0]);
      Serial.print(s1[1]);
      Serial.println(s1[2]);
      i = 0;
      j = 0;

      if (!strncmp(s1[0], "WARN", 4)) WARN = true;

      if (!strncmp(s1[0], "FIREON", 6)) f_set = atoi(s1[1]);
      else if (!strncmp(s1[0], "FIREOFF", 7)) f_set = 0;

      if (!strncmp(s1[0], "VALVEON", 7)) VALVE = true;
      else if (!strncmp(s1[0], "VALVEOFF", 8)) VALVE = false;

      if (!strncmp(s1[0], "SAFEON", 6)) SAFE = true;
      else if (!strncmp(s1[0], "SAFEOFF", 7)) SAFE = false;

      lcd.clear();
    }
  }
}
void WRITE(char s[]) {
  String msg = Serial.readStringUntil('\n');
  msg.trim();

  if (!strncmp(s, "FIRE", 4)) {

    BTSerial.print("[ALLMSG]");
    BTSerial.print(s);
    if (f_set) {
      BTSerial.print("ON@");
      BTSerial.print(f_set);
    } else BTSerial.print("OFF");
    BTSerial.write('\n');

    Serial.print("WRITE MSG : ");
    Serial.print("[ALLMSG]");
    Serial.print(s);
    if (f_set) {
      Serial.print("ON@");
      Serial.print(f_set);
    } else Serial.print("OFF");
    Serial.println();
  } else if (!strncmp(s, "CLOCK", 5)) {
    BTSerial.print("[ALLMSG]");
    BTSerial.print(s);
    BTSerial.print("@");
    BTSerial.print(clock);
    BTSerial.write('\n');

    Serial.print("WRITE MSG : ");
    Serial.print("[ALLMSG]");
    Serial.print(s);
    Serial.print("@");
    Serial.print(clock);
    Serial.write('\n');
  } else if (!strncmp(s, "VALVE", 5)) {
    BTSerial.print("[ALLMSG]");
    BTSerial.print(s);
    BTSerial.write('\n');

    Serial.print("WRITE MSG : ");
    Serial.print("[ALLMSG]");
    Serial.print(s);
    Serial.write('\n');
  } else if (!strncmp(s, "SAFE", 4)) {
    BTSerial.print("[ALLMSG]");
    BTSerial.print(s);
    BTSerial.write('\n');

    Serial.print("WRITE MSG : ");
    Serial.print("[ALLMSG]");
    Serial.print(s);
    Serial.write('\n');
  }
}
void time_clock() {
  clock--;
  //Serial.print("time : ");
  //Serial.println(clock);
  if (clock <= 0) {
    clock = 0;
    f_set = 0;
    Serial.println("time : end");
    MsTimer2::stop();
    onestop = true;
  }
}
void kEY() {
  char key = keypad.getKey();
  if (key) {
    if (key == 'H') clock += 3600;
    else if (key == 'h') clock -= 3600;
    else if (key == 'M') clock += 60;
    else if (key == 'm') clock -= 60;
    else if (key == 'S') clock+=10;
    else if (key == 's') clock-=10;

    else if (key == 'A') {
      if (f_set == 0) clock = 0;
      WRITE("FIRE");
      if (clock != 0) {
        MsTimer2::start();
        WRITE("CLOCK");
      }
    } else if (key == 'B') {
      f_set = 0;
      WRITE("FIRE");
      VALVE = !VALVE;
      if (VALVE) WRITE("VALVEON");
      else if (!VALVE) WRITE("VALVEOFF");

    } else if (key == '0') {
      SAFE = !SAFE;
      if (SAFE) WRITE("SAFEON");
      else if (!SAFE) WRITE("SAFEOFF");
    } else if (key == '1') f_set = 0;

    else if (key == '2') {
      f_set--;
      if (f_set < 0) f_set++;

    } else if (key == '3') {
      f_set++;
      if (f_set > 8) f_set--;
    }
    if (clock < 0) clock = 0;
    lcd.clear();
  }
}
void setup() {
  Serial.begin(9600);
  BTSerial.begin(9600);
  lcd.init();
  lcd.backlight();
  MsTimer2::set(1000, time_clock);
}
void loop() {
  READ();
  kEY();
  LCD(clock, f_set, VALVE);
  if (onestop) {
    WRITE("FIRE");
    onestop = false;
  }
}