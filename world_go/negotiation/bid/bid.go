package bid

type (
	Bid struct {
		AgentID   string `json:"agent_id" form:"agent_id" query:"agent_id"`
		SessionID string `json:"session_id" form:"session_id" query:"session_id"`
		Bid       string `json:"bid" form:"bid" query:"bid"`
		Type      string `json:"type" form:"type" query:"type"`
	}
	BidData struct {
		Path    string
		Token   int
		AgentID string
	}
)
