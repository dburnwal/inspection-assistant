import { useRef, useState, useCallback } from 'react'

const API_BASE = import.meta.env.VITE_API_BASE ?? ''
const API = `${API_BASE}/api/inspection/sessions`

const SEVERITY_COLOR = { LOW: '#4caf50', MEDIUM: '#ff9800', HIGH: '#f44336' }
const STATUS_COLOR = { OBSERVED: '#90caf9', CONFIRMED: '#4caf50', DISMISSED: '#888' }

const FRAME_INTERVAL_MS = 2500

export default function App() {
  const videoRef = useRef(null)
  const canvasRef = useRef(null)
  const intervalRef = useRef(null)
  const inFlightRef = useRef(false)

  const [step, setStep] = useState('form')   // form | inspecting | report
  const [sessionId, setSessionId] = useState(null)
  const [findings, setFindings] = useState([])
  const [guidance, setGuidance] = useState(null)
  const [status, setStatus] = useState('Ready')
  const [report, setReport] = useState(null)
  const [cameraError, setCameraError] = useState(null)

  const [vehicle, setVehicle] = useState({ make: '', model: '', year: '', variant: '', city: '' })

  const startCamera = async () => {
    setCameraError(null)
    try {
      const constraints = {
        video: {
          facingMode: { ideal: 'environment' },
          width: { ideal: 1280 },
          height: { ideal: 720 }
        }
      }
      const stream = await navigator.mediaDevices.getUserMedia(constraints)
      videoRef.current.srcObject = stream
    } catch (err) {
      setCameraError('Camera unavailable: ' + err.message)
    }
  }

  const stopCamera = () => {
    const stream = videoRef.current?.srcObject
    stream?.getTracks().forEach(t => t.stop())
    if (videoRef.current) videoRef.current.srcObject = null
  }

  const captureFrame = useCallback(() => {
    const video = videoRef.current
    const canvas = canvasRef.current
    if (!video || !canvas || video.readyState < 2) return null
    canvas.width = 640
    canvas.height = 480
    canvas.getContext('2d').drawImage(video, 0, 0, 640, 480)
    return new Promise(resolve => canvas.toBlob(resolve, 'image/jpeg', 0.75))
  }, [])

  const startInspection = async () => {
    setFindings([])
    setReport(null)
    setGuidance(null)

    const body = {
      profileId: 'CAR_DAMAGE',
      vehicle: vehicle.make ? vehicle : null
    }
    const res = await fetch(API, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) })
    if (!res.ok) { setStatus('Failed to create session'); return }
    const { sessionId: sid } = await res.json()
    setSessionId(sid)
    setStep('inspecting')
    setStatus('Inspection started — point camera at the car')
    await startCamera()

    intervalRef.current = setInterval(async () => {
      if (inFlightRef.current) return
      inFlightRef.current = true
      setStatus('Analyzing…')
      try {
        const blob = await captureFrame()
        if (!blob) { setStatus('Camera not ready'); return }
        const form = new FormData()
        form.append('image', blob, 'frame.jpg')
        const r = await fetch(`${API}/${sid}/frames`, { method: 'POST', body: form })
        if (r.status === 404) {
          clearInterval(intervalRef.current)
          setStatus('Session expired — start again')
          return
        }
        if (!r.ok) { setStatus('Analysis error'); return }
        const data = await r.json()
        if (data.guidance?.message) setGuidance(data.guidance)
        if (data.findings?.length) {
          setFindings(prev => {
            const ids = new Set(prev.map(f => f.id))
            const fresh = data.findings.filter(f => !ids.has(f.id))
            return [...prev.map(p => data.findings.find(f => f.id === p.id) ?? p), ...fresh]
          })
          setStatus(`${data.findings.length} finding(s) detected`)
        } else {
          setStatus('No new damage detected')
        }
      } catch {
        setStatus('Network error — retrying…')
      } finally {
        inFlightRef.current = false
      }
    }, FRAME_INTERVAL_MS)
  }

  const stopInspection = () => {
    clearInterval(intervalRef.current)
    setStatus('Inspection paused')
  }

  const finishInspection = async () => {
    clearInterval(intervalRef.current)
    stopCamera()
    if (!sessionId) return
    const res = await fetch(`${API}/${sessionId}/complete`, { method: 'POST' })
    if (!res.ok) { setStatus('Failed to generate report'); return }
    const data = await res.json()
    setReport(data)
    setStep('report')
    setStatus('Inspection complete')
  }

  const restart = () => {
    setStep('form')
    setFindings([])
    setReport(null)
    setGuidance(null)
    setSessionId(null)
    setStatus('Ready')
  }

  return (
    <div style={s.app}>
      <header style={s.header}>
        <span style={s.logo}>🔍</span> AI Car Inspection
      </header>

      {step === 'form' && (
        <div style={s.form}>
          <h2 style={s.formTitle}>Car Damage Inspection</h2>
          <p style={s.formSub}>Optionally enter vehicle details for better cost estimates</p>
          <div style={s.grid}>
            {['make', 'model', 'year', 'variant', 'city'].map(field => (
              <input key={field} style={s.input} placeholder={field.charAt(0).toUpperCase() + field.slice(1)}
                value={vehicle[field]} onChange={e => setVehicle(v => ({ ...v, [field]: e.target.value }))} />
            ))}
          </div>
          <button style={s.btnPrimary} onClick={startInspection}>Start Inspection</button>
        </div>
      )}

      {step === 'inspecting' && (
        <div style={s.inspectLayout}>
          <div style={s.cameraWrap}>
            <video ref={videoRef} autoPlay playsInline muted style={s.video} />
            <canvas ref={canvasRef} style={{ display: 'none' }} />
            {cameraError && <div style={s.cameraError}>{cameraError}</div>}

            {/* Guidance overlay */}
            {guidance?.message && (
              <div style={s.guidanceOverlay}>
                <span style={s.guidanceIcon}>📍</span> {guidance.message}
              </div>
            )}

            {/* Status badge */}
            <div style={s.statusBadge}>{status}</div>

            {/* Live finding chips */}
            <div style={s.findingChips}>
              {findings.slice(-3).map(f => (
                <div key={f.id} style={{ ...s.chip, borderColor: SEVERITY_COLOR[f.severity] }}>
                  <span style={{ color: SEVERITY_COLOR[f.severity], fontWeight: 700 }}>{f.type.replace(/_/g, ' ')}</span>
                  <span style={s.chipPart}>{f.part?.replace(/_/g, ' ')}</span>
                  <span style={{ ...s.chipStatus, color: STATUS_COLOR[f.status] }}>{f.status}</span>
                </div>
              ))}
            </div>
          </div>

          <div style={s.sidebar}>
            <div style={s.sidebarControls}>
              <button style={s.btnSmall} onClick={stopInspection}>⏸ Pause</button>
              <button style={{ ...s.btnSmall, background: '#1565c0' }} onClick={startInspection}>▶ Resume</button>
              <button style={{ ...s.btnSmall, background: '#b71c1c' }} onClick={finishInspection}>✅ Finish</button>
            </div>
            <h3 style={s.sideTitle}>Live Findings ({findings.length})</h3>
            {findings.length === 0 && <p style={s.empty}>No findings yet — scan the car</p>}
            {findings.map(f => <FindingCard key={f.id} f={f} />)}
          </div>
        </div>
      )}

      {step === 'report' && report && (
        <div style={s.report}>
          <h2 style={s.reportTitle}>Inspection Report</h2>
          {vehicle.make && (
            <p style={s.vehicleInfo}>{vehicle.make} {vehicle.model} {vehicle.year}</p>
          )}
          <div style={s.costSummary}>
            <span style={s.costLabel}>Estimated Repair Cost</span>
            <span style={s.costRange}>₹{report.totalMinCost.toLocaleString('en-IN')} – ₹{report.totalMaxCost.toLocaleString('en-IN')}</span>
            <span style={s.costDisclaimer}>{report.disclaimer}</span>
          </div>
          <h3 style={s.sideTitle}>Confirmed Findings ({report.findings.length})</h3>
          {report.findings.length === 0 && <p style={s.empty}>No confirmed damage found.</p>}
          {report.findings.map(f => <FindingCard key={f.id} f={f} showCost />)}
          <button style={{ ...s.btnPrimary, marginTop: 24 }} onClick={restart}>New Inspection</button>
        </div>
      )}
    </div>
  )
}

function FindingCard({ f, showCost }) {
  return (
    <div style={s.card}>
      <div style={s.cardHeader}>
        <span style={s.cardType}>{f.type.replace(/_/g, ' ')}</span>
        <span style={{ ...s.badge, background: SEVERITY_COLOR[f.severity] }}>{f.severity}</span>
      </div>
      <p style={s.cardPart}>{f.part?.replace(/_/g, ' ')}</p>
      <p style={s.cardDesc}>{f.description}</p>
      <div style={s.cardMeta}>
        <span>Confidence: {Math.round(f.confidence * 100)}%</span>
        <span style={{ color: STATUS_COLOR[f.status] }}>{f.status}</span>
        {f.observationCount > 1 && <span>Seen {f.observationCount}×</span>}
      </div>
      {f.guidanceMessage && <p style={s.cardGuidance}>→ {f.guidanceMessage}</p>}
      {showCost && f.costEstimate && (
        <div style={s.costBadge}>
          ₹{f.costEstimate.minimum.toLocaleString('en-IN')} – ₹{f.costEstimate.maximum.toLocaleString('en-IN')}
        </div>
      )}
    </div>
  )
}

const s = {
  app: { fontFamily: 'system-ui, sans-serif', background: '#0f0f0f', color: '#eee', minHeight: '100vh', display: 'flex', flexDirection: 'column' },
  header: { background: '#1a1a2e', padding: '14px 20px', fontSize: 18, fontWeight: 700, display: 'flex', alignItems: 'center', gap: 8, borderBottom: '1px solid #333' },
  logo: { fontSize: 22 },

  form: { maxWidth: 480, margin: '60px auto', padding: '0 20px', display: 'flex', flexDirection: 'column', gap: 16 },
  formTitle: { margin: 0, fontSize: 22, color: '#90caf9' },
  formSub: { margin: 0, color: '#888', fontSize: 13 },
  grid: { display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 },
  input: { background: '#1e1e1e', border: '1px solid #333', borderRadius: 6, padding: '10px 12px', color: '#eee', fontSize: 14, outline: 'none' },
  btnPrimary: { background: '#1565c0', color: '#fff', border: 'none', padding: '12px 24px', borderRadius: 8, cursor: 'pointer', fontWeight: 700, fontSize: 15 },

  inspectLayout: { display: 'flex', flex: 1, flexDirection: 'row', '@media(maxWidth:768px)': { flexDirection: 'column' } },
  cameraWrap: { flex: 1, position: 'relative', background: '#000', minHeight: 300 },
  video: { width: '100%', height: '100%', objectFit: 'cover', display: 'block' },
  cameraError: { position: 'absolute', top: '50%', left: '50%', transform: 'translate(-50%,-50%)', background: 'rgba(183,28,28,0.9)', padding: '12px 20px', borderRadius: 8, textAlign: 'center' },
  guidanceOverlay: { position: 'absolute', top: 12, left: '50%', transform: 'translateX(-50%)', background: 'rgba(21,101,192,0.92)', padding: '8px 16px', borderRadius: 20, fontSize: 13, fontWeight: 600, whiteSpace: 'nowrap', maxWidth: '90%', overflow: 'hidden', textOverflow: 'ellipsis' },
  statusBadge: { position: 'absolute', bottom: 12, left: 12, background: 'rgba(0,0,0,0.75)', padding: '4px 12px', borderRadius: 12, fontSize: 12 },
  findingChips: { position: 'absolute', bottom: 12, right: 12, display: 'flex', flexDirection: 'column', gap: 6, alignItems: 'flex-end' },
  chip: { background: 'rgba(0,0,0,0.82)', border: '1px solid', borderRadius: 8, padding: '6px 10px', fontSize: 12, display: 'flex', gap: 8, alignItems: 'center' },
  chipPart: { color: '#aaa' },
  chipStatus: { fontSize: 10, fontWeight: 700 },

  sidebar: { width: 300, background: '#161616', padding: 14, overflowY: 'auto', borderLeft: '1px solid #2a2a2a', display: 'flex', flexDirection: 'column', gap: 8 },
  sidebarControls: { display: 'flex', gap: 6, flexWrap: 'wrap' },
  btnSmall: { background: '#2e7d32', color: '#fff', border: 'none', padding: '7px 12px', borderRadius: 6, cursor: 'pointer', fontWeight: 600, fontSize: 12 },
  sideTitle: { margin: '8px 0 4px', fontSize: 13, color: '#90caf9' },
  empty: { color: '#555', fontSize: 12 },

  card: { background: '#1e1e1e', borderRadius: 8, padding: 12, marginBottom: 8 },
  cardHeader: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 4 },
  cardType: { fontWeight: 700, fontSize: 13 },
  badge: { fontSize: 10, padding: '2px 8px', borderRadius: 10, color: '#fff', fontWeight: 700 },
  cardPart: { fontSize: 11, color: '#90caf9', margin: '2px 0' },
  cardDesc: { fontSize: 12, color: '#bbb', margin: '4px 0' },
  cardMeta: { display: 'flex', gap: 10, fontSize: 11, color: '#888' },
  cardGuidance: { fontSize: 11, color: '#80cbc4', margin: '4px 0 0' },
  costBadge: { marginTop: 6, background: '#1b5e20', borderRadius: 6, padding: '4px 10px', fontSize: 13, fontWeight: 700, color: '#a5d6a7', display: 'inline-block' },

  report: { maxWidth: 600, margin: '0 auto', padding: '24px 20px', display: 'flex', flexDirection: 'column', gap: 12 },
  reportTitle: { margin: 0, fontSize: 22, color: '#90caf9' },
  vehicleInfo: { margin: 0, color: '#aaa', fontSize: 14 },
  costSummary: { background: '#1b2a1b', border: '1px solid #2e7d32', borderRadius: 10, padding: '16px 20px', display: 'flex', flexDirection: 'column', gap: 4 },
  costLabel: { fontSize: 12, color: '#888', textTransform: 'uppercase', letterSpacing: 1 },
  costRange: { fontSize: 26, fontWeight: 700, color: '#a5d6a7' },
  costDisclaimer: { fontSize: 11, color: '#666', marginTop: 4 },
}
