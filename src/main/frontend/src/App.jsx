import { useRef, useState, useEffect, useCallback } from 'react'

const API = '/api/inspection'

const SEVERITY_COLOR = { LOW: '#4caf50', MEDIUM: '#ff9800', HIGH: '#f44336' }

export default function App() {
  const videoRef = useRef(null)
  const canvasRef = useRef(null)
  const intervalRef = useRef(null)
  const inFlightRef = useRef(false)

  const [sessionId, setSessionId] = useState(null)
  const [findings, setFindings] = useState([])
  const [status, setStatus] = useState('Idle')
  const [report, setReport] = useState(null)

  const startCamera = async () => {
    const stream = await navigator.mediaDevices.getUserMedia({ video: true })
    videoRef.current.srcObject = stream
    setStatus('Camera ready')
  }

  useEffect(() => { startCamera() }, [])

  const captureFrame = useCallback(() => {
    const video = videoRef.current
    const canvas = canvasRef.current
    if (!video || !canvas) return null
    canvas.width = 640
    canvas.height = 480
    canvas.getContext('2d').drawImage(video, 0, 0, 640, 480)
    return new Promise(resolve => canvas.toBlob(resolve, 'image/jpeg', 0.7))
  }, [])

  const startInspection = async () => {
    setReport(null)
    setFindings([])
    const res = await fetch(`${API}/session`, { method: 'POST' })
    const { sessionId: sid } = await res.json()
    setSessionId(sid)
    setStatus('Inspecting')

    intervalRef.current = setInterval(async () => {
      if (inFlightRef.current) return
      inFlightRef.current = true
      setStatus('Analyzing frame…')
      try {
        const blob = await captureFrame()
        if (!blob) return
        const form = new FormData()
        form.append('image', blob, 'frame.jpg')
        const r = await fetch(`${API}/${sid}/frame`, { method: 'POST', body: form })
        if (r.status === 404) {
          // Backend restarted — session lost, create a new one
          clearInterval(intervalRef.current)
          setStatus('Session expired — click Start Inspection again')
          setSessionId(null)
          return
        }
        const data = await r.json()
        if (data.findings?.length) {
          setFindings(prev => {
            const ids = new Set(prev.map(f => f.id))
            const fresh = data.findings.filter(f => !ids.has(f.id))
            return [...prev, ...fresh]
          })
          setStatus('Finding detected')
        } else {
          setStatus('No visible issues')
        }
      } catch {
        setStatus('Error analyzing frame')
      } finally {
        inFlightRef.current = false
      }
    }, 1500)
  }

  const stopInspection = () => {
    clearInterval(intervalRef.current)
    setStatus('Inspection stopped')
  }

  const generateReport = async () => {
    if (!sessionId) return
    const res = await fetch(`${API}/${sessionId}/report`, { method: 'POST' })
    const data = await res.json()
    setReport(data.findings)
    setStatus('Report generated')
  }

  const displayList = report ?? findings

  return (
    <div style={styles.app}>
      <header style={styles.header}>AI LIVE INSPECTOR</header>
      <div style={styles.body}>
        <div style={styles.cameraPane}>
          <video ref={videoRef} autoPlay playsInline style={styles.video} />
          <canvas ref={canvasRef} style={{ display: 'none' }} />
          <div style={styles.statusBadge}>{status}</div>
        </div>
        <div style={styles.findingsPane}>
          <h3 style={styles.findingsTitle}>{report ? 'Report' : 'Live Findings'}</h3>
          {displayList.length === 0 && <p style={styles.noFindings}>No findings yet</p>}
          {displayList.map(f => (
            <div key={f.id} style={styles.card}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span style={styles.type}>{f.type.replace(/_/g, ' ')}</span>
                <span style={{ ...styles.badge, background: SEVERITY_COLOR[f.severity] }}>{f.severity}</span>
              </div>
              <p style={styles.desc}>{f.description}</p>
              <p style={styles.meta}>Confidence: {Math.round(f.confidence * 100)}%</p>
              <p style={styles.action}>→ {f.action}</p>
            </div>
          ))}
        </div>
      </div>
      <div style={styles.controls}>
        <button style={styles.btn} onClick={startInspection}>Start Inspection</button>
        <button style={styles.btn} onClick={stopInspection}>Stop Inspection</button>
        <button style={{ ...styles.btn, background: '#1565c0' }} onClick={generateReport}>Generate Report</button>
      </div>
    </div>
  )
}

const styles = {
  app: { fontFamily: 'sans-serif', background: '#121212', color: '#eee', minHeight: '100vh', display: 'flex', flexDirection: 'column' },
  header: { background: '#1e1e1e', padding: '12px 24px', fontSize: 20, fontWeight: 700, letterSpacing: 2, textAlign: 'center', borderBottom: '1px solid #333' },
  body: { display: 'flex', flex: 1, gap: 0 },
  cameraPane: { flex: 1, position: 'relative', background: '#000', display: 'flex', alignItems: 'center', justifyContent: 'center' },
  video: { width: '100%', maxHeight: '70vh', objectFit: 'cover' },
  statusBadge: { position: 'absolute', bottom: 12, left: 12, background: 'rgba(0,0,0,0.7)', padding: '4px 10px', borderRadius: 4, fontSize: 13 },
  findingsPane: { width: 320, background: '#1e1e1e', padding: 16, overflowY: 'auto', borderLeft: '1px solid #333' },
  findingsTitle: { margin: '0 0 12px', fontSize: 15, color: '#90caf9' },
  noFindings: { color: '#666', fontSize: 13 },
  card: { background: '#2a2a2a', borderRadius: 6, padding: 12, marginBottom: 10 },
  type: { fontWeight: 600, fontSize: 13, color: '#fff' },
  badge: { fontSize: 11, padding: '2px 8px', borderRadius: 10, color: '#fff', fontWeight: 600 },
  desc: { fontSize: 12, color: '#ccc', margin: '6px 0 4px' },
  meta: { fontSize: 11, color: '#aaa', margin: '2px 0' },
  action: { fontSize: 12, color: '#80cbc4', margin: '4px 0 0' },
  controls: { display: 'flex', gap: 12, padding: 16, background: '#1e1e1e', borderTop: '1px solid #333', justifyContent: 'center' },
  btn: { background: '#2e7d32', color: '#fff', border: 'none', padding: '10px 20px', borderRadius: 6, cursor: 'pointer', fontWeight: 600, fontSize: 14 },
}
