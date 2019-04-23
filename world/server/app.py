from flask import Flask, render_template

from worlds.grid_world import GridWorld
from entities.agent import Agent

# todo load grid world on thread
# todo implement grid world on Py
E = GridWorld()

backend = Flask(__name__)
fow_size = 7


@backend.route('/', methods=['GET'])
def home():
    return render_template('index.html')


@backend.route('/register')
def register_agent():
    # todo register agent to the world
    # create new agent
    # add agent to the world
    # return position of agent from environment object
    return


@backend.route('/fov')
def fov():
    # todo get fov
    return


@backend.route('/move')
def move():
    # todo move agent
    return


if __name__ == '__main__':
    from os import environ

    backend.debug = False
    host = environ.get('IP', 'localhost')
    port = int(environ.get('PORT', 8080))
    backend.run(host=host, port=port)
