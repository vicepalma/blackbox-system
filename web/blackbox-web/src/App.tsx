import { useState, useEffect } from "react";
import { MapContainer, TileLayer, Polyline, Marker, Popup } from "react-leaflet";
import { LatLngExpression, Icon } from "leaflet";
import axios from "axios";
import "leaflet/dist/leaflet.css"; // ✅ Importar estilos de Leaflet

interface Journey {
  journey_id: string;
  latitude: number;
  longitude: number;
  speed_kmh: number;
  timestamp: number;
}

const App = () => {
  const [journeys, setJourneys] = useState<Journey[]>([]);
  const [journeyIds, setJourneyIds] = useState<string[]>([]);
  const [selectedJourney, setSelectedJourney] = useState<string>("");

  // Cargar todos los journeyIds al inicio
  useEffect(() => {
    axios.get("http://localhost:8080/journeys")
      .then(response => {
        const uniqueIds = [...new Set(response.data.map((j: Journey) => j.journey_id))];
        setJourneyIds(uniqueIds);
      })
      .catch(error => console.error("Error cargando journeys:", error));
  }, []);

  // Cargar los datos del journey seleccionado
  useEffect(() => {
    if (selectedJourney) {
      axios.get(`http://localhost:8080/journeys/${selectedJourney}`)
        .then(response => {
          setJourneys(response.data);
        })
        .catch(error => console.error("Error cargando datos:", error));
    }
  }, [selectedJourney]);

  // Calcular centro del mapa (primer punto del recorrido o valor por defecto)
  const center: LatLngExpression = journeys.length > 0
    ? [journeys[0].latitude, journeys[0].longitude] // ✅ Tomamos el primer punto del recorrido
    : [-37.1182, -72.0131]; // ✅ Fallback si no hay datos

  // Convertir datos en posiciones para la Polyline (invertir coordenadas)
  const polylinePositions: LatLngExpression[] = journeys.map(j => [j.latitude, j.longitude]);

  // Icono personalizado para el punto de inicio
  const startIcon = new Icon({
    iconUrl: "https://leafletjs.com/examples/custom-icons/leaf-red.png",
    iconSize: [25, 41],
    iconAnchor: [12, 41]
  });

  return (
    <div style={{ height: "100vh", display: "flex", flexDirection: "column" }}>
      <header style={{ padding: "10px", textAlign: "center", background: "#282c34", color: "white" }}>
        <h2>📍 BlackBox - Rutas Registradas</h2>
        <select 
          onChange={(e) => setSelectedJourney(e.target.value)}
          style={{ padding: "5px", marginTop: "10px" }}
        >
          <option value="">Selecciona un Journey</option>
          {journeyIds.map(id => (
            <option key={id} value={id}>{id}</option>
          ))}
        </select>
      </header>

      <MapContainer center={center} zoom={15} style={{ height: "90%", width: "100%" }}>
        <TileLayer url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />
        
        {/* 📌 Dibujar la ruta */}
        {journeys.length > 0 && (
          <Polyline positions={polylinePositions} pathOptions={{ color: "blue" }} />
        )}

        {/* 📌 Marcar el punto de inicio del recorrido */}
        {journeys.length > 0 && (
          <Marker position={polylinePositions[0]} icon={startIcon}>
            <Popup>📍 Inicio del Recorrido</Popup>
          </Marker>
        )}
      </MapContainer>
    </div>
  );
};

export default App;
