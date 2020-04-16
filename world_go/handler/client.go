package handler

import (
	"github.com/gorilla/websocket"
	"sync"
)

type (
	Client struct {
		conn    *websocket.Conn
		updates chan Status
	}

	ClientMap struct {
		sync.RWMutex
		m map[*Client]bool
	}
)
