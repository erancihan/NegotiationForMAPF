from datetime import datetime

import jsons
import redis
from flask import Flask, jsonify, render_template, request
from flask_socketio import SocketIO, emit

from negotiation import negotiation_notify
from structs import Move, Notify
from world import world_list, world_move

r = redis.Redis(host='localhost', port=6379)

app = Flask(__name__, template_folder='./templates')
socketio = SocketIO(app)


@app.route("/")
def home():
    resp = {
        "time": datetime.now().timestamp(),
        "message": "Welcome!"
    }

    return render_template('index.html')


@app.route("/worlds", methods=['GET'])
def get_world_list():
    return jsonify(world_list(r))


@app.route("/move", methods=['POST'])
def post_world_move():
    req = jsons.load(request.get_json(), Move)

    world_move(req, r)
    return '', 200


@app.route("/negotiation/notify", methods=['POST'])
def post_negotiation_notify():
    req = jsons.load(request.get_json(), Notify)

    negotiation_notify(req, r)

    return '', 200


if __name__ == '__main__':
    socketio.run(app)
