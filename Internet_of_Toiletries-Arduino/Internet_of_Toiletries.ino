#include "HX711.h"
#include <SPI.h>
#include <WiFi.h>

WiFiClient client;
char clientServer[] = "shrouded-citadel-97257.herokuapp.com";
IPAddress ip(192,168,2,3);

char ssid[] = "PedroPrototype";     //  your network SSID (name) 
char pass[] = "theneedle";    // your network password
int status = WL_IDLE_STATUS;

HX711 scale1;//(A1,A0);
HX711 scale2;//(A3,A2);

void setup() {
  Serial.begin(9600);

  if (WiFi.status() == WL_NO_SHIELD) {
    Serial.println("WiFi shield not present"); 
    // don't continue:
    while(true);
  } 
  
  // attempt to connect to Wifi network:
  while ( status != WL_CONNECTED) { 
    Serial.print("Attempting to connect to SSID: ");
    Serial.println(ssid);
    // Connect to WPA/WPA2 network. Change this line if using open or WEP network:    
    status = WiFi.begin(ssid, pass);
//    status = WiFi.begin(ssid);

    // wait 10 seconds for connection:
    delay(20000);
  }   
  printWifiStatus();


    // parameter "gain" is ommited; the default value 128 is used by the library
  // HX711.DOUT  - pin #A1
  // HX711.PD_SCK - pin #A0
  scale1.begin(A1, A0);
  scale2.begin(A3, A2);

  scale1.set_scale(2280.f);                      // this value is obtained by calibrating the scale with known weights; see the README for details
  scale2.tare();               // reset the scale to 0
  scale1.set_scale(2280.f);                      // this value is obtained by calibrating the scale with known weights; see the README for details
  scale2.tare();      
}


void loop() {
  Serial.print("one reading:\t");
  Serial.print(scale1.get_units(10), 1);
  Serial.print("two reading:\t");
  Serial.println(scale2.get_units(10), 1);

  
  String PostData = "{\"product1weight\":" + String(scale1.get_units(10), 1) + ",\"product2weight\":" + String(scale2.get_units(10), 1) + " }";
    Serial.println(PostData);

  if (client.connect(clientServer, 80)) {  
    Serial.println("connected");
    Serial.println("POST /updatevalue HTTP/1.1");
    client.println("POST /updatevalue HTTP/1.1");
    client.println("Host: shrouded-citadel-97257.herokuapp.com");
    client.println("User-Agent: Internet of Toiltries");
    client.println("Content-Type: application/json");
    client.print("Content-Length: ");
    client.println(PostData.length());
    client.println();
    client.println(PostData);
    client.println();
    client.println("Connection: close");
  
    while(client.connected() && !client.available()) delay(1); //waits for data
    while (client.connected() && client.available()) { //connected or data available
      char c = client.read(); //gets byte from ethernet buffer
      Serial.print(c); //prints byte to serial monitor 
    }
    Serial.println();
    Serial.println("disconnecting.");
    Serial.println("==================");
    Serial.println();
    client.stop(); //stop client
  }

  scale1.power_down();             // put the ADC in sleep mode
  scale2.power_down();             // put the ADC in sleep mode
  delay(5000);
  scale1.power_up();
  scale2.power_up();
}

void printWifiStatus() {
  // print the SSID of the network you're attached to:
  Serial.print("SSID: ");
  Serial.println(WiFi.SSID());

  // print your WiFi shield's IP address:
  ip = WiFi.localIP();
  Serial.print("IP Address: ");
  Serial.println(ip);

  // print the received signal strength:
  long rssi = WiFi.RSSI();
  Serial.print("signal strength (RSSI):");
  Serial.print(rssi);
  Serial.println(" dBm");
  
    // check firmware version
  Serial.print("Firmware version: ");
  Serial.println(WiFi.firmwareVersion());
}
