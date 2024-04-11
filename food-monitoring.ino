#include <ESP8266WiFi.h>
#include <DHT.h>
#include <Firebase_ESP_Client.h>
#include <addons/TokenHelper.h>

#define DHTPIN D1 // Digital pin to which DHT11 sensor data pin is connected
#define DHTTYPE DHT11 // Type of DHT sensor
#define WIFI_SSID "your_ssid" // Replace with your WiFi SSID
#define WIFI_PASSWORD "wifi_password" // Replace with your WiFi password

// Firebase project configuration (replace with your details)
#define API_KEY "your_web_api_key"
#define FIREBASE_PROJECT_ID "your_project_id" // Replace with your Firebase project URL

#define USER_EMAIL "email"
#define USER_PASSWORD "password"

DHT dht(DHTPIN, DHTTYPE); // Initialize DHT sensor
const int mq2Pin = A0; // Analog pin to which MQ-2 sensor AO pin is connected
const float RL_VALUE = 5.0; // Value of the load resistance on the MQ2 board
const float RO_CLEAN_AIR_FACTOR = 9.83; // RO_CLEAR_AIR_FACTOR=(Sensor resistance in clean air)/RO
const int CALIBRATION_SAMPLE_TIMES = 50; // Number of samples used for calibration
const int CALIBRATION_SAMPLE_INTERVAL = 500; // Time interval between calibration samples (ms)
const int READ_SAMPLE_INTERVAL = 50; // Time interval between MQ2 sensor reads (ms)
const int READ_SAMPLE_TIMES = 5; // Number of samples to take in a read cycle

FirebaseData fbdo;
FirebaseAuth auth;
FirebaseConfig config;

void setup() {
  Serial.begin(115200); // Start serial communication
  dht.begin(); // Initialize DHT sensor

  // Connect to WiFi
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  while (WiFi.status() != WL_CONNECTED) {
    delay(3000);
    Serial.println("Connecting to WiFi...");
  }
  Serial.println("Connected to WiFi");

  Serial.printf("Firebase Client v%s\n\n", FIREBASE_CLIENT_VERSION);

  config.api_key = API_KEY;

  // assign the user sign-in credentials
  auth.user.email = USER_EMAIL;
  auth.user.password = USER_PASSWORD;
  
  config.token_status_callback = tokenStatusCallback;
  
  Firebase.begin(&config, &auth);
  Firebase.reconnectWiFi(true);
}

void loop() {
  String documentPath = "Sensors/Data";
  FirebaseJson content;
  
  // Read temperature and humidity from DHT sensor
  float temperature = dht.readTemperature();
  float humidity = dht.readHumidity();

  
  float mq_ratio = MQRead() / MQCalibration();
  float methaneConcentration = MQGetGasPercentage(mq_ratio, 0); // Calculate methane concentration

  // Print sensor readings
  Serial.print("Methane Concentration (ppm): ");
  Serial.printf("%.5f\n", methaneConcentration);
  Serial.print("Temperature (°C): ");
  Serial.println(temperature);
  Serial.print("Humidity (%): ");
  Serial.println(humidity);

  // Check if any reads failed and exit early (to try again)
  if (!isnan(temperature) && !isnan(humidity) && !isnan(methaneConcentration)) {
    content.set("fields/Temperature/stringValue", String(temperature, 2));
    content.set("fields/Humidity/stringValue", String(humidity, 2));
    content.set("fields/Methane_Concentration/doubleValue", methaneConcentration);

    Serial.print("Update/Add Data....");

    if(Firebase.Firestore.patchDocument(&fbdo, FIREBASE_PROJECT_ID, "", documentPath.c_str(), content.raw(), "Temperature") 
    && Firebase.Firestore.patchDocument(&fbdo, FIREBASE_PROJECT_ID, "", documentPath.c_str(), content.raw(), "Humidity")
    && Firebase.Firestore.patchDocument(&fbdo, FIREBASE_PROJECT_ID, "", documentPath.c_str(), content.raw(), "Methane_Concentration"))
    {
      Serial.printf("ok\n%s\n\n", fbdo.payload().c_str());
    }else {
      Serial.println(fbdo.errorReason());
    }
  }else {
    Serial.println("Failed to read data");
  }
  delay(30000); // Delay for stability
}

float MQResistanceCalculation(int raw_adc) {
  return (((float)RL_VALUE * (1023 - raw_adc) / raw_adc));
}

float MQCalibration() {
  int i;
  float val=0;
  for (i=0; i<CALIBRATION_SAMPLE_TIMES; i++) {
    val += MQResistanceCalculation(analogRead(mq2Pin));
    delay(CALIBRATION_SAMPLE_INTERVAL);
  }
  val = val / CALIBRATION_SAMPLE_TIMES;
  val = val / RO_CLEAN_AIR_FACTOR;
  return val;
}

float MQRead() {
  int i;
  float rs = 0;
  for (i=0; i<READ_SAMPLE_TIMES; i++) {
    rs += MQResistanceCalculation(analogRead(mq2Pin));
    delay(READ_SAMPLE_INTERVAL);
  }
  rs = rs / READ_SAMPLE_TIMES;
  return rs;
}

float MQGetGasPercentage(float rs_ro_ratio, int gas_id) {
  if ( gas_id == 0 ) {
    return (pow(10,((log10(rs_ro_ratio)-0.47)/-0.54)));
  }
  return 0;
}
