package bid

type (
	Contract struct {
		Ox  string `json:"Ox" form:"Ox" query:"Ox" redis:"Ox"`
		X   string `json:"x" form:"x" query:"x" redis:"x"`
		ETa string `json:"ETa" form:"ETa" query:"ETa" redis:"ETa"`
		A   string `json:"a" form:"a" query:"a" redis:"A"`
		ETb string `json:"ETb" form:"ETb" query:"ETb" redis:"ETb"`
		B   string `json:"b" form:"b" query:"b" redis:"B"`
	}
)
