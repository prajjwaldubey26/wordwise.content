import { useRef, useState } from 'react';
import { Alert, Button, Card, Table } from 'react-bootstrap';
import client, { errorMessage } from '../api/client';
import PageIntro from '../components/PageIntro';

export default function PlagiarismChecker() {
  const [text, setText] = useState('');
  const [file, setFile] = useState(null);
  const [result, setResult] = useState(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const fileRef = useRef(null);

  const onFileChange = (event) => {
    const next = event.target.files?.[0];
    if (!next) return;
    const isPdf =
      next.type === 'application/pdf' || /\.pdf$/i.test(next.name);
    if (!isPdf) {
      setError('Please upload a PDF file.');
      event.target.value = '';
      return;
    }
    if (next.size > 10 * 1024 * 1024) {
      setError('PDF must be 10MB or smaller.');
      event.target.value = '';
      return;
    }
    setError('');
    setFile(next);
  };

  const clearFile = () => {
    setFile(null);
    if (fileRef.current) fileRef.current.value = '';
  };

  const submit = async (event) => {
    event.preventDefault();
    setBusy(true);
    setError('');
    try {
      let data;
      if (file) {
        const form = new FormData();
        form.append('file', file);
        const response = await client.post('/plagiarism/check-upload', form);
        data = response.data;
      } else {
        const response = await client.post('/plagiarism/check', { text });
        data = response.data;
      }
      setResult(data);
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  const tone = result?.score < 25 ? 'original' : result?.score < 60 ? 'overlap' : 'risk';
  const canSubmit = file || text.trim().split(/\s+/).filter(Boolean).length >= 5;

  return (
    <>
      <PageIntro eyebrow="LOCAL ORIGINALITY CHECK" title="Know what’s already been said.">
        Paste text or upload a PDF. This checker runs locally against drafts, chapter summaries, and documents in your
        workspace.
      </PageIntro>

      <Card className="tool-card">
        <Card.Body className="p-4">
          <form onSubmit={submit}>
            <div className="d-flex flex-wrap gap-2 align-items-center mb-3">
              <input
                ref={fileRef}
                type="file"
                accept=".pdf,application/pdf"
                hidden
                onChange={onFileChange}
              />
              <Button
                type="button"
                variant="outline-success"
                className="plagiarism-upload-btn"
                onClick={() => fileRef.current?.click()}
                disabled={busy}
              >
                Upload PDF
              </Button>
              {file ? (
                <div className="chat-file-preview mb-0 flex-grow-1">
                  <span>📄 {file.name}</span>
                  <button type="button" onClick={clearFile} aria-label="Remove PDF">
                    ×
                  </button>
                </div>
              ) : (
                <span className="small text-muted">Optional · text-based PDFs up to 10MB</span>
              )}
            </div>

            <textarea
              className="form-control checker-input"
              rows="9"
              placeholder={
                file
                  ? 'PDF selected — you can still paste extra notes here, or leave this blank and check the PDF.'
                  : 'Paste a document with at least five words…'
              }
              value={text}
              onChange={(e) => setText(e.target.value)}
              maxLength="50000"
              disabled={Boolean(file)}
              required={!file}
            />

            <div className="d-flex justify-content-between align-items-center mt-3">
              <span className="small text-muted">
                {file
                  ? 'Checking uploaded PDF'
                  : `${text.trim() ? text.trim().split(/\s+/).length : 0} words`}
              </span>
              <Button className="primary-button" type="submit" disabled={busy || !canSubmit}>
                {busy ? 'Comparing…' : 'Check originality'}
              </Button>
            </div>
          </form>
          {error && (
            <Alert className="mt-3 mb-0" variant="danger">
              {error}
            </Alert>
          )}
        </Card.Body>
      </Card>

      {result && (
        <Card className={`result-card ${tone} mt-4`}>
          <Card.Body className="p-4 p-md-5">
            <div className="score-layout">
              <div className="score-circle">
                <strong>{result.score}%</strong>
                <span>similarity</span>
              </div>
              <div>
                <p className="eyebrow">SCAN COMPLETE</p>
                <h2>{result.verdict}</h2>
                <p>Your score combines Jaccard set overlap and cosine similarity across five-word phrases.</p>
              </div>
            </div>
            {result.matches.length > 0 ? (
              <>
                <h3 className="mt-5">Closest matching sources</h3>
                <Table responsive className="match-table">
                  <thead>
                    <tr>
                      <th>Source</th>
                      <th className="text-end">Similarity</th>
                    </tr>
                  </thead>
                  <tbody>
                    {result.matches.map((match) => (
                      <tr key={`${match.label}-${match.id}`}>
                        <td>{match.label}</td>
                        <td className="text-end fw-bold">{match.score}%</td>
                      </tr>
                    ))}
                  </tbody>
                </Table>
              </>
            ) : (
              <div className="no-matches">No overlapping 5-word sequences were found in your saved corpus.</div>
            )}
          </Card.Body>
        </Card>
      )}
    </>
  );
}
