package main

import (
	"fmt"
	"log"
	"net/http"

	"github.com/gin-contrib/cors"
	"github.com/gin-gonic/gin"
	"gorm.io/driver/sqlite"
	"gorm.io/gorm"
)

// Journey representa un viaje en la base de datos
type Journey struct {
	ID        uint    `json:"id" gorm:"primaryKey"`
	JourneyID string  `json:"journey_id" gorm:"column:journeyId"`
	Latitude  float64 `json:"latitude"`
	Longitude float64 `json:"longitude"`
	Speed     float64 `json:"speed"`
	Timestamp int64   `json:"timestamp"`
}

// Global DB instance
var db *gorm.DB

func main() {
	// Conectar a la base de datos SQLite
	var err error
	db, err = gorm.Open(sqlite.Open("../android/blackbox_database_latest.sqlite"), &gorm.Config{})
	if err != nil {
		log.Fatal("❌ No se pudo conectar a la base de datos:", err)
	}

	// Iniciar servidor con Gin
	r := gin.Default()

	// ✅ Habilitar CORS para permitir peticiones desde el frontend
	r.Use(cors.New(cors.Config{
		AllowOrigins:     []string{"http://127.0.0.1:5173", "http://localhost:5173"},
		AllowMethods:     []string{"GET", "POST", "PUT", "DELETE", "OPTIONS"},
		AllowHeaders:     []string{"Origin", "Content-Type", "Authorization"},
		ExposeHeaders:    []string{"Content-Length"},
		AllowCredentials: true,
	}))

	// Rutas
	r.GET("/", func(c *gin.Context) {
		c.JSON(http.StatusOK, gin.H{"message": "API de BlackBox funcionando 🚀"})
	})

	r.GET("/journeys", getJourneys)

	r.GET("/journeys/:id", getJourneyById)

	// Levantar el servidor
	fmt.Println("🚀 Servidor corriendo en http://localhost:8080")
	r.Run(":8080")
}

// getJourneys devuelve todos los viajes registrados en la BD
func getJourneys(c *gin.Context) {
	var journeys []Journey
	db.Find(&journeys)
	c.JSON(http.StatusOK, journeys)
}

func getJourneyById(c *gin.Context) {
	id := c.Param("id")
	var journeys []Journey
	db.Where("journeyId = ?", id).Find(&journeys)

	if len(journeys) == 0 {
		c.JSON(http.StatusNotFound, gin.H{"error": "No se encontraron datos para este journeyId"})
		return
	}

	c.JSON(http.StatusOK, journeys)
}
