from flask import Flask , request, jsonify
import pickle
import numpy as np

model = pickle.load(open('appmodel.pkl', 'rb'))

app = Flask(__name__)

@app.route('/')
def home():
    return  "Hello world"

@app.route('/predict', methods=['POST'])
def predict():
    temperature = request.form.get('temperature')
    humidity = request.form.get('humidity')
    methane_concentration = request.form.get('methane_concentration')

    input_query = np.array([[temperature, humidity, methane_concentration]])
    result = model.predict(input_query)[0]
    return jsonify(f"Remaining Lifespan in hours: {str(result)}")
