import { useState } from 'react';
import { Alert, Button, Card, Col, Form, Row } from 'react-bootstrap';
import client, { errorMessage } from '../api/client';
import PageIntro from '../components/PageIntro';
import LogoMark from '../components/LogoMark';
import FormattedContent from '../components/FormattedContent';

export default function ContentGenerator() {
  const [form, setForm] = useState({
    prompt: '',
    tone: 'neutral',
    contentType: 'blog',
    targetWordCount: 350,
  });
  const [result, setResult] = useState(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [copied, setCopied] = useState(false);

  const submit = async (event) => {
    event.preventDefault();
    setBusy(true);
    setError('');
    try {
      const { data } = await client.post('/content/generate', form);
      setResult(data);
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  const copy = async () => {
    await navigator.clipboard.writeText(result.content);
    setCopied(true);
    setTimeout(() => setCopied(false), 1800);
  };

  return (
    <>
      <PageIntro eyebrow="AI CONTENT GENERATOR" title="From brief to first draft.">
        Choose the format and voice, then give the idea some direction. The default mock provider works offline.
      </PageIntro>
      <Row className="g-4 align-items-start">
        <Col lg={5}>
          <Card className="tool-card">
            <Card.Body className="p-4">
              <Form onSubmit={submit}>
                <Form.Group className="mb-4">
                  <Form.Label>What would you like to write?</Form.Label>
                  <Form.Control
                    as="textarea"
                    rows={6}
                    placeholder="e.g. A practical article on building better study habits"
                    required
                    value={form.prompt}
                    onChange={(e) => setForm({ ...form, prompt: e.target.value })}
                  />
                </Form.Group>
                <Row>
                  <Col sm={6}>
                    <Form.Group className="mb-3">
                      <Form.Label>Tone</Form.Label>
                      <Form.Select
                        value={form.tone}
                        onChange={(e) => setForm({ ...form, tone: e.target.value })}
                      >
                        {['neutral', 'formal', 'casual', 'persuasive'].map((x) => (
                          <option key={x}>{x}</option>
                        ))}
                      </Form.Select>
                    </Form.Group>
                  </Col>
                  <Col sm={6}>
                    <Form.Group className="mb-3">
                      <Form.Label>Content type</Form.Label>
                      <Form.Select
                        value={form.contentType}
                        onChange={(e) => setForm({ ...form, contentType: e.target.value })}
                      >
                        {['article', 'essay', 'blog', 'email', 'story'].map((x) => (
                          <option key={x}>{x}</option>
                        ))}
                      </Form.Select>
                    </Form.Group>
                  </Col>
                </Row>
                <Form.Group className="mb-4">
                  <div className="d-flex justify-content-between">
                    <Form.Label>Target length</Form.Label>
                    <span className="small text-muted">{form.targetWordCount} words</span>
                  </div>
                  <Form.Range
                    min="50"
                    max="1200"
                    step="50"
                    value={form.targetWordCount}
                    onChange={(e) => setForm({ ...form, targetWordCount: Number(e.target.value) })}
                  />
                </Form.Group>
                {error && <Alert variant="danger">{error}</Alert>}
                <Button className="primary-button w-100" type="submit" disabled={busy}>
                  {busy ? 'Creating your draft…' : 'Generate content →'}
                </Button>
              </Form>
            </Card.Body>
          </Card>
        </Col>
        <Col lg={7}>
          {result ? (
            <Card className="output-card">
              <Card.Body className="p-4 p-md-5">
                <div className="d-flex justify-content-between align-items-start gap-3 mb-4">
                  <div>
                    <p className="eyebrow">GENERATED DRAFT</p>
                    <h2>
                      {result.contentType} in a {result.tone} tone
                    </h2>
                    <span className="small text-muted">{result.wordCount} words</span>
                  </div>
                  <Button variant="outline-success" size="sm" onClick={copy}>
                    {copied ? 'Copied!' : 'Copy'}
                  </Button>
                </div>
                <FormattedContent content={result.content} />
              </Card.Body>
            </Card>
          ) : (
            <div className="empty-state">
              <span>
                <LogoMark />
              </span>
              <h3>Your draft will appear here.</h3>
              <p>Start with an idea, set your preferences, and we’ll make a clear first pass.</p>
            </div>
          )}
        </Col>
      </Row>
    </>
  );
}
