import logging
from datetime import datetime

import jsons
import redis
from flask import Flask, jsonify, render_template, request
from flask_socketio import SocketIO, emit

from negotiation import negotiation_notify
from structs import Move, Notify
from world import world_list, world_move, world_socket, world_create, world_listen, world_delete

r = redis.Redis(host='localhost', port=6379, encoding='utf-8', decode_responses=True)

app = Flask(__name__, template_folder='./templates')
socketio = SocketIO(app)


@app.route("/")
def home():
    resp = {
        "time": datetime.now().timestamp(),
        "message": "Welcome!"
    }

    return render_template('index.html')


@app.route("/world/create", methods=['POST'])
def create_world():
    return jsonify(world_create(request.get_json(), r))


@app.route("/world/delete", methods=['POST'])
def delete_world():
    return jsonify(world_delete(request.get_json(), r))


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


@socketio.on('world_listen', '/world')
def on_world_listen(message):
    resp = world_listen(message["world_id"], r)

    emit('sync_world_listen', resp)


@socketio.on('world_state', '/world')
def on_get_world_state(message):
    resp = world_socket(message['world_id'], message['agent_id'], r)

    emit('sync_world_state', resp)


"""
========================================================================================================================
Negotiation Handlers
========================================================================================================================
"""
"""
@socketio.on('negotiation_state', '/negotiation')
def on_get_negotiation_state(message):
    resp = negotiation_socket(message['world_id'], message['session_id'], message['agent_id'], r)

    emit('sync_negotiation_state', resp)

@socketio.on('respond_to_make_action', '/negotiation')
def on_respond_to_make_action(message):
    agent_respond_to_make_action(message, r)

@socketio.on('negotiation_agent_ready', '/negotiation')
def on_agent_ready_for_negotiation(message):
    agent_ready_for_negotiation(message, r)
"""

if __name__ == '__main__':
    logging.getLogger('socketio').setLevel(logging.ERROR)
    logging.getLogger('engineio').setLevel(logging.ERROR)

    socketio.run(app)
