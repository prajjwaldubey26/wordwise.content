import { useEffect, useState } from 'react';
import { Button, Modal, Nav, Spinner, Tab, Table } from 'react-bootstrap';
import client from '../api/client';
import SoftCard from '../components/ui/SoftCard';
import TypePill from '../components/ui/TypePill';
import LogoMark from '../components/LogoMark';
import FormattedContent from '../components/FormattedContent';

const formatDate = (value) => (value ? new Date(value).toLocaleString() : '');

export default function History() {
  const [data, setData] = useState(null);
  const [selected, setSelected] = useState(null);
  const [copied, setCopied] = useState(false);

  useEffect(() => {
    Promise.all([
      client.get('/content/history'),
      client.get('/plagiarism/history'),
      client.get('/chapters/history'),
    ])
      .then(([generations, checks, chapters]) =>
        setData({
          generations: generations.data,
          checks: checks.data,
          chapters: chapters.data,
        })
      )
      .catch(() => setData({ generations: [], checks: [], chapters: [] }));
  }, []);

  const openGeneration = (item) => {
    setCopied(false);
    setSelected({
      kind: 'generation',
      title: item.prompt,
      meta: `${item.contentType} · ${item.tone} · ${item.wordCount} words`,
      body: item.content,
      date: item.createdAt,
    });
  };

  const openChapter = (item) => {
    setCopied(false);
    setSelected({
      kind: 'chapter',
      title: item.filename,
      meta: 'Chapter summary',
      body: item.summary,
      date: item.createdAt,
    });
  };

  const copyBody = async () => {
    if (!selected?.body) return;
    await navigator.clipboard.writeText(selected.body);
    setCopied(true);
    setTimeout(() => setCopied(false), 1800);
  };

  return (
    <>
      <div className="page-intro">
        <p className="eyebrow">Your archive</p>
        <h1>Everything you’ve explored.</h1>
        <p className="intro-copy">
          Revisit past drafts, originality checks, and chapter summaries. Tap any draft to read the full content.
        </p>
      </div>

      {!data ? (
        <div className="text-center py-5">
          <Spinner animation="border" />
        </div>
      ) : (
        <Tab.Container defaultActiveKey="generations">
          <Nav variant="pills" className="history-tabs mb-4">
            <Nav.Item>
              <Nav.Link eventKey="generations">Drafts ({data.generations.length})</Nav.Link>
            </Nav.Item>
            <Nav.Item>
              <Nav.Link eventKey="checks">Checks ({data.checks.length})</Nav.Link>
            </Nav.Item>
            <Nav.Item>
              <Nav.Link eventKey="chapters">Summaries ({data.chapters.length})</Nav.Link>
            </Nav.Item>
          </Nav>

          <Tab.Content>
            <Tab.Pane eventKey="generations">
              <SoftCard className="history-card">
                <div className="card-body">
                  {data.generations.length ? (
                    data.generations.map((item) => (
                      <article
                        className="history-item history-item-clickable"
                        key={item.id}
                        role="button"
                        tabIndex={0}
                        onClick={() => openGeneration(item)}
                        onKeyDown={(event) => {
                          if (event.key === 'Enter' || event.key === ' ') {
                            event.preventDefault();
                            openGeneration(item);
                          }
                        }}
                      >
                        <div>
                          <div className="d-flex align-items-center gap-2 mb-2">
                            <TypePill type="generation" />
                            <span className="small text-muted">
                              {item.contentType} · {item.tone}
                            </span>
                          </div>
                          <h3>{item.prompt}</h3>
                          <p>{item.content.slice(0, 220)}…</p>
                          <span className="history-open-hint">Open full draft →</span>
                        </div>
                        <small>
                          {item.wordCount} words
                          <br />
                          {formatDate(item.createdAt)}
                        </small>
                      </article>
                    ))
                  ) : (
                    <Empty text="Your generated drafts will appear here." />
                  )}
                </div>
              </SoftCard>
            </Tab.Pane>

            <Tab.Pane eventKey="checks">
              <SoftCard className="history-card">
                <div className="card-body">
                  {data.checks.length ? (
                    <Table responsive className="mb-0">
                      <thead>
                        <tr>
                          <th>Type</th>
                          <th>Verdict</th>
                          <th>Score</th>
                          <th>Checked</th>
                        </tr>
                      </thead>
                      <tbody>
                        {data.checks.map((item) => (
                          <tr key={item.id}>
                            <td>
                              <TypePill type="check" />
                            </td>
                            <td>{item.verdict}</td>
                            <td className="fw-bold">{item.score}%</td>
                            <td>{formatDate(item.createdAt)}</td>
                          </tr>
                        ))}
                      </tbody>
                    </Table>
                  ) : (
                    <Empty text="Your originality checks will appear here." />
                  )}
                </div>
              </SoftCard>
            </Tab.Pane>

            <Tab.Pane eventKey="chapters">
              <SoftCard className="history-card">
                <div className="card-body">
                  {data.chapters.length ? (
                    data.chapters.map((item) => (
                      <article
                        className="history-item history-item-clickable"
                        key={item.id}
                        role="button"
                        tabIndex={0}
                        onClick={() => openChapter(item)}
                        onKeyDown={(event) => {
                          if (event.key === 'Enter' || event.key === ' ') {
                            event.preventDefault();
                            openChapter(item);
                          }
                        }}
                      >
                        <div>
                          <div className="mb-2">
                            <TypePill type="summary" />
                          </div>
                          <h3>{item.filename}</h3>
                          <p>{item.summary.slice(0, 220)}…</p>
                          <span className="history-open-hint">Open full summary →</span>
                        </div>
                        <small>{formatDate(item.createdAt)}</small>
                      </article>
                    ))
                  ) : (
                    <Empty text="Your chapter summaries will appear here." />
                  )}
                </div>
              </SoftCard>
            </Tab.Pane>
          </Tab.Content>
        </Tab.Container>
      )}

      <Modal show={Boolean(selected)} onHide={() => setSelected(null)} size="lg" centered scrollable>
        {selected && (
          <>
            <Modal.Header closeButton>
              <Modal.Title>
                <p className="eyebrow mb-1">{selected.meta}</p>
                <span>{selected.title}</span>
              </Modal.Title>
            </Modal.Header>
            <Modal.Body>
              <p className="small text-muted mb-3">{formatDate(selected.date)}</p>
              <FormattedContent className="history-detail-text" content={selected.body} />
            </Modal.Body>
            <Modal.Footer>
              <Button variant="outline-secondary" onClick={() => setSelected(null)}>
                Close
              </Button>
              <Button className="primary-button" onClick={copyBody}>
                {copied ? 'Copied!' : 'Copy'}
              </Button>
            </Modal.Footer>
          </>
        )}
      </Modal>
    </>
  );
}

function Empty({ text }) {
  return (
    <div className="empty-inline">
      <LogoMark />
      <p>{text}</p>
    </div>
  );
}
