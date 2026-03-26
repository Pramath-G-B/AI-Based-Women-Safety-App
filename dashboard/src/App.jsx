import { MapContainer, TileLayer, Marker, Popup, useMap } from "react-leaflet";
import "leaflet/dist/leaflet.css";
import { useState, useEffect } from "react";
import L from "leaflet";
import { db } from "./firebase";
import { collection, getDocs } from "firebase/firestore";

// ── STYLES ──
const styles = `
:root {
  --bg:#121212; --surface:#1e1e1e; --surface2:#252525; --border:rgba(255,255,255,0.1);
  --sos:#ff3a3a; --sos-glow:rgba(255,58,58,0.4);
  --panic:#ffcc00; --panic-glow:rgba(255,204,0,0.4);
  --text:#fff; --muted:#aaa;
}
body{margin:0;font-family:sans-serif;background:var(--bg);}
.sentinel-wrap{display:flex;flex-direction:column;height:100vh;color:var(--text);}
.s-header{display:flex;align-items:center;justify-content:space-between;padding:0 20px;height:58px;background:var(--surface);border-bottom:1px solid var(--border);}
.s-logo{display:flex;align-items:center;gap:12px;}
.s-logo-icon{width:34px;height:34px;background:linear-gradient(135deg,#4f8cff,#7b5ea7);display:flex;align-items:center;justify-content:center;font-weight:800;color:#fff;border-radius:6px;}
.s-logo-text{font-weight:700;letter-spacing:2px;text-transform:uppercase;}
.s-main{display:flex;flex:1;overflow:hidden;}
.s-map-wrap{flex:1;position:relative;z-index:0;}
.leaflet-container{height:100%;width:100%;z-index:0;}
.s-aside{width:360px;flex-shrink:0;background:var(--surface2);border-left:1px solid var(--border);display:flex;flex-direction:column;z-index:1;}
.s-sidebar-head{padding:16px;border-bottom:1px solid var(--border);}
.s-sidebar-label{font-size:0.62rem;letter-spacing:2px;color:var(--muted);margin-bottom:4px;}
.s-count-row{display:flex;align-items:baseline;gap:8px;}
.s-count{font-size:2rem;font-weight:800;}
.s-count-label{font-size:0.8rem;color:var(--muted);}
.s-tabs{display:flex;gap:6px;padding:10px;border-bottom:1px solid var(--border);overflow-x:auto;}
.s-tab{padding:5px 12px;border-radius:20px;border:1px solid var(--border);background:transparent;color:var(--muted);cursor:pointer;white-space:nowrap;}
.s-tab.active{color:var(--text);border-color:rgba(255,255,255,0.3);}
.s-tab.sos.active{color:var(--sos);border-color:var(--sos);}
.s-tab.panic.active{color:var(--panic);border-color:var(--panic);}
.s-list{flex:1;overflow-y:auto;padding:10px;display:flex;flex-direction:column;gap:8px;}
.s-card{background:var(--surface);border:1px solid var(--border);border-radius:10px;padding:12px;cursor:pointer;position:relative;}
.s-card.sos{border-left:3px solid var(--sos);}
.s-card.panic{border-left:3px solid var(--panic);}
.s-card:hover{transform:translateX(2px);border-color:rgba(255,255,255,0.3);}
.s-card.selected{background:#333;border-color:rgba(79,140,255,0.4);}
.s-card-top{display:flex;align-items:center;justify-content:space-between;margin-bottom:8px;}
.s-badge{font-size:0.62rem;font-weight:700;padding:3px 8px;border-radius:4px;}
.s-badge.sos{background:var(--sos-glow);color:var(--sos);}
.s-badge.panic{background:var(--panic-glow);color:var(--panic);}
.s-bar-wrap{height:3px;background:rgba(255,255,255,0.08);border-radius:3px;margin-bottom:8px;overflow:hidden;}
.s-bar{height:100%;border-radius:3px;transition:width 0.5s;}
.s-bar.sos{background:var(--sos);}
.s-bar.panic{background:var(--panic);}
.s-card-bottom{display:flex;align-items:center;justify-content:space-between;gap:8px;flex-wrap:wrap;}
.s-time{font-size:0.65rem;color:var(--muted);}
.s-remove{padding:4px 8px;border-radius:5px;border:1px solid var(--border);background:transparent;color:var(--muted);font-size:0.6rem;cursor:pointer;}
.s-remove:hover{color:var(--sos);border-color:var(--sos);}
.s-detail{border-top:1px solid var(--border);padding:16px;background:var(--surface2);max-height:260px;overflow-y:auto;}
.s-detail-title{font-size:0.65rem;color:var(--muted);margin-bottom:10px;}
.s-detail-grid{display:grid;grid-template-columns:1fr 1fr;gap:10px;}
.s-detail-item{background:var(--surface);border-radius:6px;padding:10px;border:1px solid var(--border);}
.s-detail-item.full{grid-column:1/-1;}
.s-detail-label{font-size:0.58rem;color:var(--muted);margin-bottom:4px;}
.s-detail-value{font-weight:700;color:var(--text);}
.s-empty,.s-loading,.s-error{padding:16px;color:var(--muted);}
`;

// ── HELPERS ──
function colorFor(trigger) {
  return { sos: "#ff3a3a", panic: "#ffcc00" }[trigger] || "#fff";
}

function normalizeTriggerType(triggerType) {
  const value = String(triggerType || "").toUpperCase();
  if (value === "SOS_BUTTON" || value.includes("SOS")) return "sos";
  if (value.includes("PANIC")) return "panic";
  return "sos";
}

function formatTimestamp(timestamp) {
  if (!timestamp) return "Unknown time";

  if (typeof timestamp === "number") {
    return new Date(timestamp).toLocaleString();
  }

  if (timestamp?.seconds) {
    return new Date(timestamp.seconds * 1000).toLocaleString();
  }

  return String(timestamp);
}

function makeIcon(alert) {
  const color = colorFor(alert.trigger);
  return L.divIcon({
    html: `<div style="width:30px;height:30px;border-radius:50%;background:${color}33;border:2px solid ${color};display:flex;align-items:center;justify-content:center;font-size:10px;font-weight:700;color:${color};font-family:'JetBrains Mono',monospace;box-shadow:0 0 12px ${color}aa;z-index:9999;">${alert.riskScore}</div>`,
    className: "",
    iconSize: [30, 30],
    iconAnchor: [15, 15]
  });
}

function FlyTo({ alerts }) {
  const map = useMap();

  useEffect(() => {
    if (alerts.length > 0) {
      map.flyTo([alerts[0].lat, alerts[0].lng], 14, { duration: 1.2 });
    }
  }, [alerts, map]);

  return null;
}

function FlyToSelected({ alert }) {
  const map = useMap();

  useEffect(() => {
    if (alert) {
      map.flyTo([alert.lat, alert.lng], 15, { duration: 0.8 });
    }
  }, [alert, map]);

  return null;
}

// ── MAIN APP ──
export default function App() {
  const [alerts, setAlerts] = useState([]);
  const [selectedId, setSelectedId] = useState(null);
  const [filter, setFilter] = useState("all");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    const fetchAlerts = async () => {
      try {
        setLoading(true);
        setError("");

        const querySnapshot = await getDocs(collection(db, "alerts"));

        const firebaseAlerts = querySnapshot.docs
          .map((doc, index) => {
            const data = doc.data();
            return {
              id: doc.id,
              localId: index,
              userId: data.userId || "Unknown",
              lat: Number(data.lat ?? 0),
              lng: Number(data.lng ?? 0),
              riskScore: Number(data.risk ?? data.riskScore ?? 0),
              trigger: normalizeTriggerType(data.triggerType),
              triggerType: data.triggerType || "UNKNOWN",
              mode: data.mode || "UNKNOWN",
              rawTrigger: data.trigger,
              timestamp: data.timestamp // keep raw for sorting
            };
          })
          .filter(alert => !Number.isNaN(alert.lat) && !Number.isNaN(alert.lng))
          .sort((a, b) => {
            const t1 = a.timestamp?.seconds ? a.timestamp.seconds : new Date(a.timestamp).getTime();
            const t2 = b.timestamp?.seconds ? b.timestamp.seconds : new Date(b.timestamp).getTime();
            return t2 - t1; // descending order
          })
          .map(alert => ({ ...alert, timestamp: formatTimestamp(alert.timestamp) })); // format timestamp after sorting

        setAlerts(firebaseAlerts);
      } catch (err) {
        console.error("Error fetching alerts:", err);
        setError("Failed to load alerts from Firebase");
      } finally {
        setLoading(false);
      }
    };

    fetchAlerts();
  }, []);

  const selectedAlert = alerts.find((a) => a.id === selectedId) || null;
  const filtered = filter === "all" ? alerts : alerts.filter((a) => a.trigger === filter);

  function removeAlert(id) {
    setAlerts((prev) => prev.filter((a) => a.id !== id));
    if (selectedId === id) setSelectedId(null);
  }

  function toggleSelect(id) {
    setSelectedId((prev) => (prev === id ? null : id));
  }

  return (
    <>
      <style>{styles}</style>
      <div className="sentinel-wrap">
        <header className="s-header">
          <div className="s-logo">
            <div className="s-logo-icon">S</div>
            <div className="s-logo-text">Sentinel</div>
          </div>
        </header>

        <div className="s-main">
          <div className="s-map-wrap">
            <MapContainer
              center={[12.9716, 77.5946]}
              zoom={13}
              style={{ height: "100%", width: "100%" }}
              zoomControl={true}
            >
              <TileLayer url="https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png" />

              {filtered.map((alert) => (
                <Marker
                  key={alert.id}
                  position={[alert.lat, alert.lng]}
                  icon={makeIcon(alert)}
                  eventHandlers={{ click: () => toggleSelect(alert.id) }}
                >
                  <Popup>
                    <div style={{ fontFamily: "'JetBrains Mono', monospace" }}>
                      <div style={{ color: colorFor(alert.trigger), fontWeight: 700 }}>
                        {alert.triggerType} — {alert.riskScore}
                      </div>
                      <div style={{ color: "#aaa", fontSize: "0.72rem" }}>
                        {alert.timestamp}
                      </div>
                      <div style={{ color: "#aaa", fontSize: "0.72rem" }}>
                        User: {alert.userId}
                      </div>
                    </div>
                  </Popup>
                </Marker>
              ))}

              <FlyTo alerts={filtered} />
              {selectedAlert && <FlyToSelected alert={selectedAlert} />}
            </MapContainer>
          </div>

          <aside className="s-aside">
            <div className="s-sidebar-head">
              <div className="s-sidebar-label">Active Incidents</div>
              <div className="s-count-row">
                <div className="s-count">{alerts.length}</div>
                <div className="s-count-label">alerts tracked</div>
              </div>
            </div>

            <div className="s-tabs">
              {["all", "sos", "panic"].map((f) => (
                <button
                  key={f}
                  className={`s-tab ${f !== "all" ? f : ""} ${filter === f ? "active" : ""}`}
                  onClick={() => setFilter(f)}
                >
                  {f.toUpperCase()}
                </button>
              ))}
            </div>

            <div className="s-list">
              {loading ? (
                <div className="s-loading">Loading alerts...</div>
              ) : error ? (
                <div className="s-error">{error}</div>
              ) : filtered.length === 0 ? (
                <div className="s-empty">No alerts in this category</div>
              ) : (
                filtered.map((alert) => {
                  const t = alert.trigger;
                  const isSelected = alert.id === selectedId;

                  return (
                    <div
                      key={alert.id}
                      className={`s-card ${t} ${isSelected ? "selected" : ""}`}
                      onClick={() => toggleSelect(alert.id)}
                    >
                      <div className="s-card-top">
                        <span className={`s-badge ${t}`}>{alert.triggerType}</span>
                        <span className={`s-risk ${t}`}>{alert.riskScore}</span>
                      </div>

                      <div className="s-bar-wrap">
                        <div className={`s-bar ${t}`} style={{ width: `${alert.riskScore}%` }} />
                      </div>

                      <div className="s-card-bottom">
                        <span className="s-time">{alert.timestamp}</span>
                        <button
                          className="s-remove"
                          onClick={(e) => {
                            e.stopPropagation();
                            removeAlert(alert.id);
                          }}
                        >
                          REMOVE
                        </button>
                      </div>
                    </div>
                  );
                })
              )}
            </div>

            {selectedAlert && (
              <div className="s-detail">
                <div className="s-detail-title">◆ Incident Details</div>

                <div className="s-detail-grid">
                  <div className="s-detail-item">
                    <div className="s-detail-label">Risk Score</div>
                    <div
                      className="s-detail-value"
                      style={{ color: colorFor(selectedAlert.trigger) }}
                    >
                      {selectedAlert.riskScore}
                    </div>
                  </div>

                  <div className="s-detail-item">
                    <div className="s-detail-label">Trigger Type</div>
                    <div className="s-detail-value">{selectedAlert.triggerType}</div>
                  </div>

                  <div className="s-detail-item full">
                    <div className="s-detail-label">Coordinates</div>
                    <div className="s-detail-value">
                      {selectedAlert.lat.toFixed(4)}, {selectedAlert.lng.toFixed(4)}
                    </div>
                  </div>

                  <div className="s-detail-item">
                    <div className="s-detail-label">Reported</div>
                    <div className="s-detail-value">{selectedAlert.timestamp}</div>
                  </div>

                  <div className="s-detail-item">
                    <div className="s-detail-label">User ID</div>
                    <div className="s-detail-value">{selectedAlert.userId}</div>
                  </div>

                  <div className="s-detail-item">
                    <div className="s-detail-label">Mode</div>
                    <div className="s-detail-value">{selectedAlert.mode}</div>
                  </div>
                </div>
              </div>
            )}
          </aside>
        </div>
      </div>
    </>
  );
}