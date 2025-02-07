import { useState, useEffect } from "react";
import { MapContainer, TileLayer, Polyline, Marker, Popup } from "react-leaflet";
import { LatLngExpression } from "leaflet";
import axios from "axios";
import "leaflet/dist/leaflet.css"; // ✅ Importar estilos de Leaflet
import "./App.css"; // ✅ Importar estilos adicionales

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
    axios.get<Journey[]>("http://localhost:8080/journeys")
      .then(response => {
        const uniqueIds: string[] = [...new Set(response.data.map((j) => j.journey_id))];
        setJourneyIds(uniqueIds);
      })
      .catch(error => console.error("Error cargando journeys:", error));
  }, []);

  // Cargar los datos del journey seleccionado
  useEffect(() => {
    if (selectedJourney) {
      axios.get<Journey[]>(`http://localhost:8080/journeys/${selectedJourney}`)
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

  return (
    <div className="container">
      <header className="header">
        <h2>📍 BlackBox - Rutas Registradas</h2>
        <select
          onChange={(e) => setSelectedJourney(e.target.value)}
          className="dropdown"
        >
          <option value="">Selecciona un Journey</option>
          {journeyIds.map(id => (
            <option key={id} value={id}>{id}</option>
          ))}
        </select>
      </header>

      <div className="map-container">
        <MapContainer center={center} zoom={15} className="map">
          <TileLayer url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />

          {/* 📌 Dibujar la ruta */}
          {journeys.length > 0 && (
            <Polyline positions={polylinePositions} pathOptions={{ color: "blue" }} />
          )}

          {/* 📌 Marcar el punto de inicio del recorrido */}
          {journeys.length > 0 && (
            <Marker position={polylinePositions[0]}>
              <Popup>📍 Inicio del Recorrido</Popup>
            </Marker>
          )}
        </MapContainer>
      </div>
    </div>
  );
};

export default App;
