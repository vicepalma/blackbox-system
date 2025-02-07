import { useState, useEffect, useCallback } from "react";
import { MapContainer, TileLayer, Polyline, Marker, Popup } from "react-leaflet";
import L, { LatLngExpression } from "leaflet";
import axios from "axios";
import "leaflet/dist/leaflet.css"; // Importar estilos de Leaflet
import "./App.css"; // Importar estilos adicionales

interface Journey {
  journey_id: string;
  latitude: number;
  longitude: number;
  speed: number;
  timestamp: number;
}

const App = () => {
  const [journeys, setJourneys] = useState<Journey[]>([]);
  const [journeyIds, setJourneyIds] = useState<string[]>([]);
  const [selectedJourney, setSelectedJourney] = useState<string>("");
  const [pastJourneys, setPastJourneys] = useState<Journey[][]>([]);

  // 🔥 Íconos alquímicos optimizados
  const fireIcon = L.icon({
    iconUrl: "https://upload.wikimedia.org/wikipedia/commons/5/5d/Alchemical_fire_symbol_%28heavy_red%29.svg",
    iconSize: [20, 20],
    iconAnchor: [15, 30],
    popupAnchor: [0, -30]
  });

  const waterIcon = L.icon({
    iconUrl: "https://upload.wikimedia.org/wikipedia/commons/2/27/Alchemical_water_symbol_%28heavy_blue%29.svg",
    iconSize: [20, 20],
    iconAnchor: [12, 25],
    popupAnchor: [0, -25]
  });

  const airIcon = L.icon({
    iconUrl: "https://upload.wikimedia.org/wikipedia/commons/d/d0/Alchemical_air_symbol.svg",
    iconSize: [20, 20],
    iconAnchor: [15, 30],
    popupAnchor: [0, -30]
  });

  const earthIcon = L.icon({
    iconUrl: "https://upload.wikimedia.org/wikipedia/commons/a/a5/Alchemical_earth_symbol_%28fixed_width%29.svg",
    iconSize: [20, 20],
    iconAnchor: [12, 25],
    popupAnchor: [0, -25]
  });

  // Función para obtener el icono y clase de resplandor
  const getAlchemyIcon = (speed: number) => {
    if (speed <= 10) return { icon: fireIcon, className: "glow-fire" };
    if (speed <= 50) return { icon: waterIcon, className: "glow-water" };
    if (speed <= 100) return { icon: airIcon, className: "glow-air" };
    return { icon: earthIcon, className: "glow-earth" };
  };


  const getColorBySpeed = (speed: number) => {
    if (speed <= 10) return "blue"; //(lento)
    if (speed <= 50) return "green"; //(normal)
    if (speed <= 100) return "yellow"; //(rápido)
    return "red"; //(extremo)
  };

  // const playSound = (soundFile: string) => {
  //   const audio = new Audio(`/sounds/${soundFile}`);
  //   audio.play();
  // };

  const savePastJourney = useCallback(() => {
    if (selectedJourney && journeys.length > 0) {
      setPastJourneys(prev => [...prev, journeys]);
    }
  }, [selectedJourney, journeys]);

  // Cargar los IDs de los journeys al iniciar
  useEffect(() => {
    axios.get<Journey[]>("http://localhost:8080/journeys")
      .then(response => {
        const uniqueIds: string[] = [...new Set(response.data.map((j) => j.journey_id))];
        setJourneyIds(uniqueIds);
      })
      .catch(error => console.error("Error cargando journeys:", error));
  }, []);

  // Cargar datos del journey seleccionado
  useEffect(() => {
    if (selectedJourney) {
      axios.get<Journey[]>(`http://localhost:8080/journeys/${selectedJourney}`)
        .then(response => {
          setJourneys(response.data);
        })
        .catch(error => console.error("Error cargando datos:", error));
    }
  }, [selectedJourney]);

  useEffect(() => {
    savePastJourney();
  }, [savePastJourney]);

  useEffect(() => {
    if (selectedJourney && journeys.length > 0) {
      //playSound("alchemy-start-01.mp3"); //Sonido cuando el viaje carga
    }
  }, [selectedJourney, journeys]);

  
  // Calcular centro del mapa
  const center: LatLngExpression = journeys.length > 0
    ? [journeys[0].latitude, journeys[0].longitude]
    : [-37.1182, -72.0131]; //Fallback

  // Convertir datos en posiciones para la Polyline
  const polylinePositions: LatLngExpression[] = journeys.map(j => [j.latitude, j.longitude]);

  return (
    <div className="container">
      <header className="header">
        <h2>📍 BlackBox - Rutas Registradas</h2>
        <select
          onChange={(e) => {
            setSelectedJourney(e.target.value);
            //playSound("journey-selected.mp3"); // Sonido al seleccionar un viaje
          }}
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

          {/*Dibujar la ruta con colores según la velocidad */}
          {journeys.length > 1 && journeys.map((journey, index) => {
            if (index === 0) return null; // No podemos conectar el primer punto con otro

            return (
              <Polyline
                key={index}
                positions={[
                  [journeys[index - 1].latitude, journeys[index - 1].longitude],
                  [journey.latitude, journey.longitude]
                ]}
                pathOptions={{ color: getColorBySpeed(journey.speed), weight: 4 }}
              />
            );
          })}


          {/* Marcar el punto de inicio */}
          {journeys.length > 0 && (
            <Marker position={polylinePositions[0]} icon={fireIcon}>
              <Popup>🔥 Inicio del Recorrido</Popup>
            </Marker>
          )}

          {/* Marcar el punto final */}
          {journeys.length > 1 && (
            <Marker position={polylinePositions[polylinePositions.length - 1]} icon={airIcon}>
              <Popup>🌪 Fin del Recorrido</Popup>
            </Marker>
          )}


          {/* Marcar puntos intermedios cada 10 registros */}
          {journeys.map((j, index) =>
            index % 10 === 0 ? (
              <Marker key={index} position={[j.latitude, j.longitude]} icon={getAlchemyIcon(j.speed).icon}>
                <Popup>
                  <span className={`glowing-marker ${getAlchemyIcon(j.speed).className}`}>
                    📍 Velocidad: {j.speed.toFixed(1)} km/h
                  </span>
                </Popup>
              </Marker>
            ) : null
          )}


          {pastJourneys.map((oldJourney, index) => (
            <Polyline
              key={`past-${index}`}
              positions={oldJourney.map(j => [j.latitude, j.longitude])}
              pathOptions={{
                color: "rgba(255, 255, 255, 0.3)", // Blanco con opacidad del 30%
                weight: 2, // Líneas más delgadas para que no compitan con la ruta actual
                dashArray: "5, 10", // Líneas punteadas para simular un rastro místico
              }}
            />
          ))}

        </MapContainer>
      </div>
    </div>
  );
};

export default App;
