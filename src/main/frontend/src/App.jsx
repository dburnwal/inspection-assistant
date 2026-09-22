import { useRef, useState, useCallback, useEffect } from 'react'

const API_BASE = import.meta.env.VITE_API_BASE ?? ''
const API = `${API_BASE}/api/inspection/sessions`
const DEMO_API = `${API_BASE}/api/demo/scenarios`

const SEVERITY_COLOR = { LOW: '#4caf50', MEDIUM: '#ff9800', HIGH: '#f44336' }
const STATUS_COLOR = { OBSERVED: '#90caf9', CONFIRMED: '#4caf50', DISMISSED: '#888' }

// ─── Mode Setup ──────────────────────────────────────────────────────────────

const FRAME_SOURCES = { CAMERA: 'CAMERA', DEMO: 'DEMO' }
const AI_PROVIDERS  = { OLLAMA: 'OLLAMA', MOCK: 'MOCK' }

// ─── App ─────────────────────────────────────────────────────────────────────

export default function App() {
  const [screen, setScreen] = useState('setup')   // setup | inspecting | report

  const [frameSource, setFrameSource] = useState(FRAME_SOURCES.DEMO)
  const [aiProvider,  setAiProvider]  = useState(AI_PROVIDERS.MOCK)
  const [scenario,    setScenario]    = useState(null)
  const [scenarios,   setScenarios]   = useState([])
  const [vehicle,     setVehicle]     = useState({ make: '', model: '', year: '', variant: '', city: '' })

  const [sessionId,  setSessionId]  = useState(null)
  const [findings,   setFindings]   = useState([])
  const [guidance,   setGuidance]   = useState(null)
  const [status,     setStatus]     = useState('Ready')
  const [report,     setReport]     = useState(null)

  useEffect(() => {
    fetch(DEMO_API).then(r => r.json()).then(setScenarios).catch(() => {})
  }, [])

  const startInspection = async () => {
    setFindings([]); setReport(null); setGuidance(null)
    const body = {
      profileId: 'CAR_DAMAGE',
      vehicle: vehicle.make ? vehicle : null
    }
    const res = await fetch(API, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) })
    if (!res.ok) { setStatus('Failed to create session'); return }
    const { sessionId: sid } = await res.json()
    setSessionId(sid)
    setScreen('inspecting')
    setStatus('Inspection started')
  }

  const onFindingsUpdate = useCallback((newFindings, newGuidance) => {
    if (newGuidance?.message) setGuidance(newGuidance)
    if (newFindings?.length) {
      setFindings(prev => {
        const ids = new Set(prev.map(f => f.id))
        const updated = prev.map(p => newFindings.find(f => f.id === p.id) ?? p)
        const fresh = newFindings.filter(f => !ids.has(f.id))
        return [...updated, ...fresh]
      })
      setStatus(`${newFindings.length} finding(s) detected`)
    } else {
      setStatus('No new damage detected')
    }
  }, [])

  const finishInspection = async () => {
    if (!sessionId) return
    const res = await fetch(`${API}/${sessionId}/complete`, { method: 'POST' })
    if (!res.ok) { setStatus('Failed to generate report'); return }
    setReport(await res.json())
    setScreen('report')
  }

  const restart = () => {
    setScreen('setup'); setFindings([]); setReport(null)
    setGuidance(null); setSessionId(null); setStatus('Ready')
  }

  return (
    <div style={s.app}>
      <header style={s.header}><span>🔍</span> AI Car Inspection</header>

      {screen === 'setup' && (
        <SetupScreen
          frameSource={frameSource} setFrameSource={setFrameSource}
          aiProvider={aiProvider}   setAiProvider={setAiProvider}
          scenarios={scenarios}     scenario={scenario} setScenario={setScenario}
          vehicle={vehicle}         setVehicle={setVehicle}
          onStart={startInspection}
        />
      )}

      {screen === 'inspecting' && (
        <InspectionScreen
          sessionId={sessionId}
          frameSource={frameSource}
          aiProvider={aiProvider}
          scenario={scenario}
          findings={findings}
          guidance={guidance}
          status={status}
          setStatus={setStatus}
          onFindingsUpdate={onFindingsUpdate}
          onFinish={finishInspection}
        />
      )}

      {screen === 'report' && report && (
        <ReportScreen report={report} vehicle={vehicle} onRestart={restart} />
      )}
    </div>
  )
}

// ─── Setup Screen ─────────────────────────────────────────────────────────────

function SetupScreen({ frameSource, setFrameSource, aiProvider, setAiProvider,
                       scenarios, scenario, setScenario, vehicle, setVehicle, onStart }) {
  const isMock = aiProvider === AI_PROVIDERS.MOCK
  const isDemo = frameSource === FRAME_SOURCES.DEMO

  return (
    <div style={s.setup}>
      <h2 style={s.setupTitle}>Car Damage Inspection</h2>

      <div style={s.modeSection}>
        <label style={s.modeLabel}>Frame Source</label>
        <div style={s.modeRow}>
          {Object.values(FRAME_SOURCES).map(fs => (
            <button key={fs} style={{ ...s.modeBtn, ...(frameSource === fs ? s.modeBtnActive : {}) }}
              onClick={() => setFrameSource(fs)}>
              {fs === FRAME_SOURCES.CAMERA ? '📷 Live Camera' : '🎬 Demo Car'}
            </button>
          ))}
        </div>

        <label style={s.modeLabel}>AI Provider</label>
        <div style={s.modeRow}>
          {Object.values(AI_PROVIDERS).map(ap => (
            <button key={ap} style={{ ...s.modeBtn, ...(aiProvider === ap ? s.modeBtnActive : {}) }}
              onClick={() => setAiProvider(ap)}>
              {ap === AI_PROVIDERS.MOCK ? '🤖 Mock AI' : '🧠 Ollama'}
            </button>
          ))}
        </div>

        <div style={s.modeHint}>
          {isDemo && isMock  && '⚡ Fastest — no car, no Ollama needed'}
          {isDemo && !isMock && '🧠 Real AI on demo images — no physical car needed'}
          {!isDemo && isMock && '📷 Test camera capture without AI'}
          {!isDemo && !isMock && '🚗 Full real-world inspection'}
        </div>
      </div>

      {isDemo && scenarios.length > 0 && (
        <div style={s.scenarioSection}>
          <label style={s.modeLabel}>Demo Scenario</label>
          <div style={s.scenarioGrid}>
            {scenarios.map(sc => (
              <button key={sc.id}
                style={{ ...s.scenarioBtn, ...(scenario?.id === sc.id ? s.scenarioBtnActive : {}) }}
                onClick={() => setScenario(sc)}>
                <span style={s.scenarioName}>{sc.name}</span>
                <span style={s.scenarioDesc}>{sc.description}</span>
              </button>
            ))}
          </div>
        </div>
      )}

      <div style={s.vehicleSection}>
        <label style={s.modeLabel}>Vehicle (optional)</label>
        <div style={s.vehicleGrid}>
          {['make', 'model', 'year', 'variant', 'city'].map(f => (
            <input key={f} style={s.input}
              placeholder={f.charAt(0).toUpperCase() + f.slice(1)}
              value={vehicle[f]}
              onChange={e => setVehicle(v => ({ ...v, [f]: e.target.value }))} />
          ))}
        </div>
      </div>

      <button style={s.btnPrimary}
        disabled={isDemo && !scenario}
        onClick={onStart}>
        {isDemo && !scenario ? 'Select a scenario to start' : 'Start Inspection'}
      </button>
    </div>
  )
}

// ─── Inspection Screen ────────────────────────────────────────────────────────

function InspectionScreen({ sessionId, frameSource, aiProvider, scenario,
                             findings, guidance, status, setStatus,
                             onFindingsUpdate, onFinish }) {
  return (
    <div style={s.inspectLayout}>
      <div style={s.viewportWrap}>
        <InspectionViewport
          sessionId={sessionId}
          frameSource={frameSource}
          aiProvider={aiProvider}
          scenario={scenario}
          guidance={guidance}
          status={status}
          findings={findings}
          setStatus={setStatus}
          onFindingsUpdate={onFindingsUpdate}
          onFinish={onFinish}
        />
      </div>
      <div style={s.sidebar}>
        <h3 style={s.sideTitle}>Live Findings ({findings.length})</h3>
        {findings.length === 0 && <p style={s.empty}>No findings yet</p>}
        {findings.map(f => <FindingCard key={f.id} f={f} />)}
      </div>
    </div>
  )
}

// ─── Inspection Viewport (shared for camera + demo) ───────────────────────────

function InspectionViewport({ sessionId, frameSource, aiProvider, scenario,
                               guidance, status, findings, setStatus,
                               onFindingsUpdate, onFinish }) {
  return (
    <div style={s.viewport}>
      {frameSource === FRAME_SOURCES.CAMERA
        ? <CameraSource sessionId={sessionId} aiProvider={aiProvider}
            setStatus={setStatus} onFindingsUpdate={onFindingsUpdate} />
        : <DemoSource sessionId={sessionId} aiProvider={aiProvider}
            scenario={scenario} setStatus={setStatus} onFindingsUpdate={onFindingsUpdate} />
      }

      {guidance?.message && (
        <div style={s.guidanceOverlay}>📍 {guidance.message}</div>
      )}
      <div style={s.statusBadge}>{status}</div>

      <div style={s.findingChips}>
        {findings.slice(-3).map(f => (
          <div key={f.id} style={{ ...s.chip, borderColor: SEVERITY_COLOR[f.severity] }}>
            <span style={{ color: SEVERITY_COLOR[f.severity], fontWeight: 700 }}>{f.type.replace(/_/g, ' ')}</span>
            <span style={s.chipPart}>{f.part?.replace(/_/g, ' ')}</span>
            <span style={{ ...s.chipStatus, color: STATUS_COLOR[f.status] }}>{f.status}</span>
          </div>
        ))}
      </div>

      <button style={s.finishBtn} onClick={onFinish}>✅ Finish Inspection</button>
    </div>
  )
}

// ─── Camera Source ────────────────────────────────────────────────────────────

function CameraSource({ sessionId, aiProvider, setStatus, onFindingsUpdate }) {
  const videoRef = useRef(null)
  const canvasRef = useRef(null)
  const intervalRef = useRef(null)
  const inFlightRef = useRef(false)
  const [cameraError, setCameraError] = useState(null)

  useEffect(() => {
    startCamera()
    return () => {
      clearInterval(intervalRef.current)
      stopCamera()
    }
  }, [])

  useEffect(() => {
    if (!sessionId) return
    intervalRef.current = setInterval(() => sendFrame(sessionId, aiProvider), 2500)
    return () => clearInterval(intervalRef.current)
  }, [sessionId, aiProvider])

  const startCamera = async () => {
    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        video: { facingMode: { ideal: 'environment' }, width: { ideal: 1280 }, height: { ideal: 720 } }
      })
      if (videoRef.current) videoRef.current.srcObject = stream
    } catch (err) {
      setCameraError('Camera unavailable: ' + err.message)
    }
  }

  const stopCamera = () => {
    videoRef.current?.srcObject?.getTracks().forEach(t => t.stop())
  }

  const sendFrame = async (sid, provider) => {
    if (inFlightRef.current) return
    inFlightRef.current = true
    setStatus('Analyzing…')
    try {
      const video = videoRef.current, canvas = canvasRef.current
      if (!video || !canvas || video.readyState < 2) return
      canvas.width = 640; canvas.height = 480
      canvas.getContext('2d').drawImage(video, 0, 0, 640, 480)
      const blob = await new Promise(r => canvas.toBlob(r, 'image/jpeg', 0.75))
      await submitFrame(sid, blob, null, provider, onFindingsUpdate, setStatus)
    } finally {
      inFlightRef.current = false
    }
  }

  return (
    <>
      {cameraError
        ? <div style={s.cameraError}>{cameraError}</div>
        : <video ref={videoRef} autoPlay playsInline muted style={s.media} />
      }
      <canvas ref={canvasRef} style={{ display: 'none' }} />
    </>
  )
}

// ─── Demo Source ──────────────────────────────────────────────────────────────

function DemoSource({ sessionId, aiProvider, scenario, setStatus, onFindingsUpdate }) {
  const [frameIndex, setFrameIndex] = useState(0)
  const [autoPlay, setAutoPlay] = useState(true)
  const [analyzing, setAnalyzing] = useState(false)
  const intervalRef = useRef(null)

  const frames = scenario?.frames ?? []
  const currentFrame = frames[frameIndex]

  const analyzeFrame = useCallback(async (frame) => {
    if (!sessionId || !frame || analyzing) return
    setAnalyzing(true)
    setStatus(`Analyzing: ${frame.label}`)
    try {
      const blob = new Blob([new Uint8Array([0xFF, 0xD8, 0xFF, 0xE0])], { type: 'image/jpeg' })
      await submitFrame(sessionId, blob, frame.frameId, aiProvider, onFindingsUpdate, setStatus)
    } finally {
      setAnalyzing(false)
    }
  }, [sessionId, aiProvider, analyzing, onFindingsUpdate, setStatus])

  useEffect(() => {
    if (autoPlay && frames.length > 0) {
      intervalRef.current = setInterval(() => {
        setFrameIndex(i => {
          const next = i < frames.length - 1 ? i + 1 : i
          return next
        })
      }, 2500)
    }
    return () => clearInterval(intervalRef.current)
  }, [autoPlay, frames.length])

  useEffect(() => {
    if (currentFrame) analyzeFrame(currentFrame)
  }, [frameIndex])

  const prev = () => setFrameIndex(i => Math.max(0, i - 1))
  const next = () => setFrameIndex(i => Math.min(frames.length - 1, i + 1))

  return (
    <div style={s.demoWrap}>
      <div style={s.demoImageArea}>
        <div style={s.demoPlaceholder}>
          <span style={s.demoIcon}>🚗</span>
          <span style={s.demoFrameLabel}>{currentFrame?.label ?? 'No frame'}</span>
          <span style={s.demoFrameId}>{currentFrame?.frameId}</span>
          {analyzing && <span style={s.demoAnalyzing}>⏳ Analyzing…</span>}
        </div>
      </div>
      <div style={s.demoControls}>
        <button style={s.demoBtn} onClick={prev} disabled={frameIndex === 0}>◀ Prev</button>
        <span style={s.demoCounter}>{frameIndex + 1} / {frames.length}</span>
        <button style={s.demoBtn} onClick={next} disabled={frameIndex === frames.length - 1}>Next ▶</button>
        <button style={{ ...s.demoBtn, background: autoPlay ? '#b71c1c' : '#2e7d32' }}
          onClick={() => setAutoPlay(a => !a)}>
          {autoPlay ? '⏸ Pause' : '▶ Auto'}
        </button>
        <button style={s.demoBtn} onClick={() => { setFrameIndex(0); setAutoPlay(true) }}>↺ Restart</button>
      </div>
    </div>
  )
}

// ─── Report Screen ────────────────────────────────────────────────────────────

function ReportScreen({ report, vehicle, onRestart }) {
  return (
    <div style={s.report}>
      <h2 style={s.reportTitle}>Inspection Report</h2>
      {vehicle.make && <p style={s.vehicleInfo}>{vehicle.make} {vehicle.model} {vehicle.year}</p>}
      <div style={s.costSummary}>
        <span style={s.costLabel}>Estimated Repair Cost</span>
        <span style={s.costRange}>₹{report.totalMinCost.toLocaleString('en-IN')} – ₹{report.totalMaxCost.toLocaleString('en-IN')}</span>
        <span style={s.costDisclaimer}>{report.disclaimer}</span>
      </div>
      <h3 style={s.sideTitle}>Confirmed Findings ({report.findings.length})</h3>
      {report.findings.length === 0 && <p style={s.empty}>No confirmed damage found.</p>}
      {report.findings.map(f => <FindingCard key={f.id} f={f} showCost />)}
      <button style={{ ...s.btnPrimary, marginTop: 24 }} onClick={onRestart}>New Inspection</button>
    </div>
  )
}

// ─── Shared Components ────────────────────────────────────────────────────────

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

// ─── Shared frame submission ──────────────────────────────────────────────────

async function submitFrame(sessionId, blob, frameId, aiProvider, onFindingsUpdate, setStatus) {
  try {
    const form = new FormData()
    form.append('image', blob, 'frame.jpg')
    const headers = {}
    if (frameId) headers['X-Frame-Id'] = frameId
    if (aiProvider === AI_PROVIDERS.MOCK) headers['X-AI-Provider'] = 'mock'

    const r = await fetch(`${API}/${sessionId}/frames`, { method: 'POST', headers, body: form })
    if (r.status === 404) { setStatus('Session expired — start again'); return }
    if (!r.ok) { setStatus('Analysis error'); return }
    const data = await r.json()
    onFindingsUpdate(data.findings, data.guidance)
  } catch {
    setStatus('Network error — retrying…')
  }
}

// ─── Styles ───────────────────────────────────────────────────────────────────

const s = {
  app: { fontFamily: 'system-ui,sans-serif', background: '#0f0f0f', color: '#eee', minHeight: '100vh', display: 'flex', flexDirection: 'column' },
  header: { background: '#1a1a2e', padding: '14px 20px', fontSize: 18, fontWeight: 700, display: 'flex', alignItems: 'center', gap: 8, borderBottom: '1px solid #333' },

  // Setup
  setup: { maxWidth: 560, margin: '40px auto', padding: '0 20px', display: 'flex', flexDirection: 'column', gap: 20 },
  setupTitle: { margin: 0, fontSize: 22, color: '#90caf9' },
  modeSection: { display: 'flex', flexDirection: 'column', gap: 8 },
  modeLabel: { fontSize: 11, color: '#888', textTransform: 'uppercase', letterSpacing: 1 },
  modeRow: { display: 'flex', gap: 8 },
  modeBtn: { flex: 1, background: '#1e1e1e', border: '1px solid #333', borderRadius: 8, padding: '10px 14px', color: '#aaa', cursor: 'pointer', fontSize: 13, fontWeight: 600 },
  modeBtnActive: { background: '#1a3a5c', border: '1px solid #1565c0', color: '#90caf9' },
  modeHint: { fontSize: 12, color: '#4caf50', padding: '6px 10px', background: '#0a1f0a', borderRadius: 6 },
  scenarioSection: { display: 'flex', flexDirection: 'column', gap: 8 },
  scenarioGrid: { display: 'flex', flexDirection: 'column', gap: 6 },
  scenarioBtn: { background: '#1e1e1e', border: '1px solid #333', borderRadius: 8, padding: '10px 14px', color: '#aaa', cursor: 'pointer', textAlign: 'left', display: 'flex', flexDirection: 'column', gap: 2 },
  scenarioBtnActive: { background: '#1a3a5c', border: '1px solid #1565c0' },
  scenarioName: { fontWeight: 700, fontSize: 13, color: '#eee' },
  scenarioDesc: { fontSize: 11, color: '#888' },
  vehicleSection: { display: 'flex', flexDirection: 'column', gap: 8 },
  vehicleGrid: { display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8 },
  input: { background: '#1e1e1e', border: '1px solid #333', borderRadius: 6, padding: '10px 12px', color: '#eee', fontSize: 14, outline: 'none' },
  btnPrimary: { background: '#1565c0', color: '#fff', border: 'none', padding: '12px 24px', borderRadius: 8, cursor: 'pointer', fontWeight: 700, fontSize: 15 },

  // Inspection layout
  inspectLayout: { display: 'flex', flex: 1 },
  viewportWrap: { flex: 1, position: 'relative', background: '#000' },
  sidebar: { width: 300, background: '#161616', padding: 14, overflowY: 'auto', borderLeft: '1px solid #2a2a2a', display: 'flex', flexDirection: 'column', gap: 8 },
  sideTitle: { margin: '4px 0', fontSize: 13, color: '#90caf9' },
  empty: { color: '#555', fontSize: 12 },

  // Viewport
  viewport: { position: 'relative', width: '100%', height: '100%', minHeight: 300 },
  media: { width: '100%', height: '100%', objectFit: 'cover', display: 'block' },
  cameraError: { display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100%', background: 'rgba(183,28,28,0.2)', color: '#ef9a9a', padding: 20, textAlign: 'center' },
  guidanceOverlay: { position: 'absolute', top: 12, left: '50%', transform: 'translateX(-50%)', background: 'rgba(21,101,192,0.92)', padding: '8px 16px', borderRadius: 20, fontSize: 13, fontWeight: 600, whiteSpace: 'nowrap', maxWidth: '90%', overflow: 'hidden', textOverflow: 'ellipsis' },
  statusBadge: { position: 'absolute', bottom: 56, left: 12, background: 'rgba(0,0,0,0.75)', padding: '4px 12px', borderRadius: 12, fontSize: 12 },
  findingChips: { position: 'absolute', top: 52, right: 12, display: 'flex', flexDirection: 'column', gap: 6, alignItems: 'flex-end' },
  chip: { background: 'rgba(0,0,0,0.82)', border: '1px solid', borderRadius: 8, padding: '6px 10px', fontSize: 12, display: 'flex', gap: 8, alignItems: 'center' },
  chipPart: { color: '#aaa' },
  chipStatus: { fontSize: 10, fontWeight: 700 },
  finishBtn: { position: 'absolute', bottom: 12, right: 12, background: '#b71c1c', color: '#fff', border: 'none', padding: '8px 16px', borderRadius: 8, cursor: 'pointer', fontWeight: 700, fontSize: 13 },

  // Demo source
  demoWrap: { width: '100%', height: '100%', display: 'flex', flexDirection: 'column' },
  demoImageArea: { flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', background: '#111' },
  demoPlaceholder: { display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 8 },
  demoIcon: { fontSize: 80 },
  demoFrameLabel: { fontSize: 18, fontWeight: 700, color: '#90caf9' },
  demoFrameId: { fontSize: 12, color: '#555' },
  demoAnalyzing: { fontSize: 13, color: '#ff9800' },
  demoControls: { display: 'flex', gap: 8, padding: '10px 12px', background: '#1a1a1a', alignItems: 'center', borderTop: '1px solid #2a2a2a', flexWrap: 'wrap' },
  demoBtn: { background: '#1e1e1e', border: '1px solid #333', color: '#eee', padding: '6px 12px', borderRadius: 6, cursor: 'pointer', fontSize: 12, fontWeight: 600 },
  demoCounter: { fontSize: 12, color: '#888', flex: 1, textAlign: 'center' },

  // Finding card
  card: { background: '#1e1e1e', borderRadius: 8, padding: 12, marginBottom: 4 },
  cardHeader: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 4 },
  cardType: { fontWeight: 700, fontSize: 13 },
  badge: { fontSize: 10, padding: '2px 8px', borderRadius: 10, color: '#fff', fontWeight: 700 },
  cardPart: { fontSize: 11, color: '#90caf9', margin: '2px 0' },
  cardDesc: { fontSize: 12, color: '#bbb', margin: '4px 0' },
  cardMeta: { display: 'flex', gap: 10, fontSize: 11, color: '#888', flexWrap: 'wrap' },
  cardGuidance: { fontSize: 11, color: '#80cbc4', margin: '4px 0 0' },
  costBadge: { marginTop: 6, background: '#1b5e20', borderRadius: 6, padding: '4px 10px', fontSize: 13, fontWeight: 700, color: '#a5d6a7', display: 'inline-block' },

  // Report
  report: { maxWidth: 600, margin: '0 auto', padding: '24px 20px', display: 'flex', flexDirection: 'column', gap: 12 },
  reportTitle: { margin: 0, fontSize: 22, color: '#90caf9' },
  vehicleInfo: { margin: 0, color: '#aaa', fontSize: 14 },
  costSummary: { background: '#1b2a1b', border: '1px solid #2e7d32', borderRadius: 10, padding: '16px 20px', display: 'flex', flexDirection: 'column', gap: 4 },
  costLabel: { fontSize: 12, color: '#888', textTransform: 'uppercase', letterSpacing: 1 },
  costRange: { fontSize: 26, fontWeight: 700, color: '#a5d6a7' },
  costDisclaimer: { fontSize: 11, color: '#666', marginTop: 4 },
}
