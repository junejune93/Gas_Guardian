#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "stm32f4xx.h"
#include "adc1.h"
#include "uart2.h"
#include "uart4.h"
#include "tim4_counter.h"
#include "tim10_motor.h"
#include "led.h"
#include "wifi.h"

/******************************************************************************
* General-purpose timers TIM10

  포트 연결:

  PA0,PA1 : UART4 RX,TX  Bluetooth Or Wifi
  PA2,PA3 : UART2 RX,TX  Debug
  PA4 : CdS // ADC1
  PA5 : Inf Ray // ADC2
  PC0~PC7 : LED
  PB8 : Servo Motor

******************************************************************************/
#define CMD_SIZE 50
#define ARR_CNT 5  

extern volatile uint16_t adc_data[2];

extern volatile unsigned char rx2Flag;
extern char rx2Data[50];
extern volatile unsigned char rx4Flag;
extern char rx4Data[50];
extern unsigned int tim1_counter;
extern volatile char uart4_rxdata[5][100];
char sendData[50]={0};  //wifi send buff
char recvData[50]={0};  //wifi recv buff  

int valve_state = 0;
int fire_detect = 0;
int fireFlag = 1;
int recv_time = 0;
int gas_usage = 0;
int gas_stage = 0;
int safe_mode = 0;
int distance = 0;
int cds_value = 0;

typedef struct{
  int msec;
  int sec;
}TIME;

int tt = 0;
int t_cnt = 0;
TIME time;
char ctime[20];
char pretime[20];

void clock_calc(){
  if(t_cnt == 10){
    t_cnt = 0;
    time.sec++;
    gas_usage += gas_stage;
    printf("gas usage : %d\r\n", gas_usage);
  }
}

void TIM3_IRQHandler(void){
  if(TIM_GetITStatus(TIM3, TIM_IT_Update) != RESET){ 
    TIM_ClearITPendingBit(TIM3, TIM_IT_Update);
    time.msec++;
    if(time.msec == 100){
      t_cnt++ ; //100ms
      time.msec = 0;
      }
    clock_calc();
  }
}

void Init_ADC(void);

int main(){
  int i;
  time.sec = 0;
  time.msec = 0;
  
  TIM_TimeBaseInitTypeDef  TIM_TimeBaseStructure;
  NVIC_InitTypeDef   NVIC_InitStructure; 
  
  RCC_APB1PeriphClockCmd(RCC_APB1Periph_TIM3, ENABLE);
  
  NVIC_PriorityGroupConfig(NVIC_PriorityGroup_2);
  NVIC_InitStructure.NVIC_IRQChannel = TIM3_IRQn;
  NVIC_InitStructure.NVIC_IRQChannelPreemptionPriority = 0x01;
  NVIC_InitStructure.NVIC_IRQChannelSubPriority = 0x01;
  NVIC_InitStructure.NVIC_IRQChannelCmd = ENABLE;
  NVIC_Init(&NVIC_InitStructure);
  
  TIM_TimeBaseStructure.TIM_Prescaler = 84-1;         //(168Mhz/2)/840 = 0.1MHz(10us)
  TIM_TimeBaseStructure.TIM_Period = 1000-1;
  TIM_TimeBaseStructure.TIM_ClockDivision = TIM_CKD_DIV1;
  TIM_TimeBaseStructure.TIM_CounterMode = TIM_CounterMode_Up;
  TIM_TimeBaseInit(TIM3, &TIM_TimeBaseStructure);
  
  TIM_ClearITPendingBit(TIM3, TIM_IT_Update);
  TIM_ITConfig(TIM3, TIM_IT_Update, ENABLE);
  
  TIM10_Motor_Init();
  PORTC_Led_Init();   
  UART2_init();
  Serial2_Send_String("start main()\r\n");
   
  UART4_init();
  Init_ADC();

  ADC_Cmd(ADC1, ENABLE);

  ADC_SoftwareStartConv(ADC1);
  
  WIFI_init();
  sprintf(sendData,"["LOGID":"PASSWD"]");  
  WIFI_send(sendData);
  
/*============================================================================*/  
/*                                 Code Start                                 */
/*============================================================================*/
  while(1){
    recvData[0] = 0;
    if(wifi_wait("+IPD","+IPD", 10)){  //수신포멧 :  +IPD,6:hello  끝문자 0x0a
      for(i=0;i<5;i++) {
        if(strncmp((char *)uart4_rxdata[i],"+IPD",4)==0) {
          strcpy(recvData,(char *)(uart4_rxdata[i]+8));
          recvData[strlen((char *)(uart4_rxdata[i]+8)) - 1] = 0;
        }
      }
    }
    if(rx2Flag){
      printf("rx2Data : %s\r\n",rx2Data);
      rx2Flag = 0;
    }  
    if(recvData[0] != 0){
      Serial4_Event();
      recvData[0] = 0;
    }
    
    cds_value = (int)(adc_data[0]*0.01);
    distance = (int)(adc_data[1]);
    
    if (safe_mode == 1){
      if (distance >= 2000){
        tt = time.sec;
      } else if ((distance < 2000)&&((tt+10) == time.sec)){
        Serial4_Event();
        gas_stage = 0;
        TIM10->CCR1 = 1500; // 서보모터 OFF
        GPIO_ResetBits(GPIOC, 0xFF);
      }
    }

    if ((cds_value >= 33) && (fireFlag == 1)){
      fire_detect = 1;
      fireFlag = 0;
    }

    if (fire_detect == 1){
      Serial4_Event();
      TIM10->CCR1 = 1500; // 서보모터 OFF
      GPIO_ResetBits(GPIOC, 0xFF);
      fire_detect = 0;
    }

    if (gas_stage != 0){
      TIM_Cmd(TIM3, ENABLE); // 타이머 시작
    } else if (gas_stage == 0){
      TIM_Cmd(TIM3, DISABLE); // 타이머 종료
    }
    if (gas_usage >= 100){
      Serial4_Event();
    }
  }
}

void Serial4_Event(){
  int i=0;
  char * pToken;
  char * pArray[ARR_CNT]={0};
  char recvBuf[CMD_SIZE]={0};

  strcpy(recvBuf,recvData);
  i=0; 
  printf("rx4Data : %s\r\n",recvBuf);   
  pToken = strtok(recvBuf,"[@]");
  
  while(pToken != NULL){
    pArray[i] = pToken;
    if(++i >= ARR_CNT)
      break;
    pToken = strtok(NULL,"[@]");
  }
  
  if(!strcmp(pArray[1],"VALVEON")) { // 밸브 on off
    TIM10->CCR1 = 1500-700; // 서보모터 ON
  }
  else if(!strcmp(pArray[1],"VALVEOFF")) {
    gas_stage = 0;
    TIM10->CCR1 = 1500; // 서보모터 OFF
    GPIO_ResetBits(GPIOC, 0xFF);
  }
  
  else if(!strcmp(pArray[1],"FIREON")) { // On : [ALLMSG]FIREON@(불 세기)1~3
//    int fire_value = (atoi)(pArray[2]);
//    void LED_ON(int fire_value);
    if(!strcmp(pArray[2],"1")) {
      gas_stage = 1;
      TIM10->CCR1 = 1500-700;
      GPIO_ResetBits(GPIOC, 0xFF);
      GPIO_SetBits(GPIOC, 0x01);  // 0000 0001
    }
    else if (!strcmp(pArray[2],"2")) {
      gas_stage = 2;
      TIM10->CCR1 = 1500-700;
      GPIO_ResetBits(GPIOC, 0xFF);
      GPIO_SetBits(GPIOC, 0x03); // 0000 0011
    }
    else if (!strcmp(pArray[2],"3")) {
      gas_stage = 3;
      TIM10->CCR1 = 1500-700;
      GPIO_ResetBits(GPIOC, 0xFF);
      GPIO_SetBits(GPIOC, 0x07);
    }
    else if (!strcmp(pArray[2],"4")) {
      gas_stage = 4;
      TIM10->CCR1 = 1500-700;
      GPIO_ResetBits(GPIOC, 0xFF);
      GPIO_SetBits(GPIOC, 0x0F);
    }
    else if (!strcmp(pArray[2],"5")) {
      gas_stage = 5;
      TIM10->CCR1 = 1500-700;
      GPIO_ResetBits(GPIOC, 0xFF);
      GPIO_SetBits(GPIOC, 0x1F);
    }
    else if (!strcmp(pArray[2],"6")) {
      gas_stage = 6;
      TIM10->CCR1 = 1500-700;
      GPIO_ResetBits(GPIOC, 0xFF);
      GPIO_SetBits(GPIOC, 0x3F);
    }
    else if (!strcmp(pArray[2],"7")) {
      gas_stage = 7;
      TIM10->CCR1 = 1500-700;
      GPIO_ResetBits(GPIOC, 0xFF);
      GPIO_SetBits(GPIOC, 0x7F);
    }
    else if (!strcmp(pArray[2],"8")) {
      gas_stage = 8;
      TIM10->CCR1 = 1500-700;
      GPIO_ResetBits(GPIOC, 0xFF);
      GPIO_SetBits(GPIOC, 0xFF); // 2 2 2 2 2 2 2 2 - 1
    }
  }
  else if(!strcmp(pArray[1],"FIREOFF")) {
    gas_stage = 0;
    TIM10->CCR1 = 1500;
    GPIO_ResetBits(GPIOC, 0xFF);
  }
  else if (!strcmp(pArray[1],"SAFEON")) {
    safe_mode = 1;
  }
  else if (!strcmp(pArray[1],"SAFEOFF")) {
    safe_mode = 0;
  }
  else if (fire_detect == 1){ // 화재 감지시 경고 보내줌
    sprintf(sendData,"[%s]%s\n","FCM","WARNING");
    WIFI_send(sendData);
    sprintf(sendData,"[%s]%s\n","ARD","WARNING");
    WIFI_send(sendData);
    sprintf(sendData,"[%s]%s\n","AND","FIREOFF");
    WIFI_send(sendData);
    sprintf(sendData,"[%s]%s\n","AND","VALVEOFF");
    WIFI_send(sendData);
    fire_detect = 0;
    return;
  }
  else if (gas_usage >= 100){
    gas_usage = gas_usage - 100;
    printf("GAS USE\r\n");
    sprintf(sendData,"[%s]%s@%s\n","DB","GASUSE","100");
    WIFI_send(sendData);
    return;
  }
  else if ((distance < 2000)&&((tt+10) == time.sec)){
    printf("SAFE\r\n");
    sprintf(sendData,"[%s]%s\n","AND","FIREOFF");
    WIFI_send(sendData);
    sprintf(sendData,"[%s]%s\n","ARD","FIREOFF");
    WIFI_send(sendData);
    return;
  }
  else if (!strncmp(pArray[1]," New conn",8)) {
    return;
  }
  else if (!strncmp(pArray[1]," Already log",8)) {
    connectWiFi();
    sprintf(sendData,"["LOGID":"PASSWD"]");    
    WIFI_send(sendData);
    return;
  }
  else
    return;
   sprintf(sendData,"[%s]%s@%s\n",pArray[0],pArray[1],pArray[2]);
   WIFI_send(sendData);
}

//void LED_ON(int fire_value){
//  gas_stage = fire_value;
//  TIM10->CCR1 = 1500-700;
//  GPIO_ResetBits(GPIOC, 0xFF);
//  int GPIO_firevalue = 0;
//  for (int i = 0; i < fire_value; i++){
//    GPIO_firevalue = fire_value * 2 - 1;
//  }
//  GPIO_SetBits(GPIOC, GPIO_firevalue);
//}