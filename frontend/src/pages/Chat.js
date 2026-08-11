import { useCallback, useEffect, useRef, useState } from 'react';
import { Form, Spinner } from 'react-bootstrap';
import { Link } from 'react-router-dom';
import client, { errorMessage } from '../api/client';
import FormattedContent from '../components/FormattedContent';
import { useAuth } from '../context/AuthContext';

const formatTime = (value) => (value ? new Date(value).toLocaleString() : '');

const ALLOWED_TYPES = [
  'application/pdf',
  'image/png',
  'image/jpeg',
  'image/jpg',
  'image/webp',
  'image/gif',
];

const SUGGESTIONS = [
  { label: 'Create a draft', prompt: 'Write a short blog draft about building better study habits' },
  { label: 'Write or edit', prompt: 'Help me rewrite this more clearly and warmly: ' },
  { label: 'Summarize a PDF', prompt: 'I will upload a PDF — please summarize the key points' },
];

function parseMessageContent(content) {
  if (!content) return { attachment: null, text: '' };
  const attachMatch = content.match(/^📎\s+(.+?)\s+\((PDF|Image)\)\s*\n\n([\s\S]*?)(?:\n\n--- Begin attached|\s*$)/);
  if (!attachMatch) {
    return { attachment: null, text: content };
  }
  const text = attachMatch[3].split('\n\n--- Begin attached')[0].trim();
  return {
    attachment: { name: attachMatch[1], kind: attachMatch[2] },
    text: text || 'Please read this file.',
  };
}

function initials(name = '') {
  const parts = name.trim().split(/\s+/).filter(Boolean);
  if (!parts.length) return 'WW';
  if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
  return `${parts[0][0]}${parts[1][0]}`.toUpperCase();
}

export default function Chat() {
  const { user } = useAuth();
  const [conversations, setConversations] = useState([]);
  const [models, setModels] = useState([]);
  const [activeId, setActiveId] = useState(null);
  const [active, setActive] = useState(null);
  const [draft, setDraft] = useState('');
  const [file, setFile] = useState(null);
  const [busy, setBusy] = useState(false);
  const [loadingList, setLoadingList] = useState(true);
  const [loadingChat, setLoadingChat] = useState(false);
  const [error, setError] = useState('');
  const [selectedModel, setSelectedModel] = useState('mock');
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const bottomRef = useRef(null);
  const textareaRef = useRef(null);
  const fileRef = useRef(null);

  const showEmpty = !loadingChat && (!active || !active.messages?.length);

  const loadConversations = useCallback(async () => {
    const { data } = await client.get('/chat/conversations');
    setConversations(data);
    return data;
  }, []);

  const openConversation = useCallback(async (id) => {
    setLoadingChat(true);
    setError('');
    setSidebarOpen(false);
    try {
      const { data } = await client.get(`/chat/conversations/${id}`);
      setActiveId(id);
      setActive(data);
      setSelectedModel((current) => data.model || current);
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setLoadingChat(false);
    }
  }, []);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const [modelRes, list] = await Promise.all([
          client.get('/chat/models'),
          loadConversations(),
        ]);
        if (cancelled) return;
        setModels(modelRes.data);
        const preferred =
          modelRes.data.find((m) => m.available && m.id !== 'mock')?.id ||
          modelRes.data.find((m) => m.available)?.id ||
          'mock';
        setSelectedModel(preferred);
        // Land on empty “Ready when you are” screen like ChatGPT (don’t auto-open a thread).
        void list;
      } catch (err) {
        if (!cancelled) setError(errorMessage(err));
      } finally {
        if (!cancelled) setLoadingList(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [loadConversations]);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth', block: 'end' });
  }, [active?.messages, busy]);

  useEffect(() => {
    const el = textareaRef.current;
    if (!el) return;
    el.style.height = '0px';
    el.style.height = `${Math.min(el.scrollHeight, 160)}px`;
  }, [draft]);

  const startNewChat = async () => {
    setActiveId(null);
    setActive(null);
    setDraft('');
    setFile(null);
    setError('');
    setSidebarOpen(false);
    textareaRef.current?.focus();
  };

  const changeModel = async (model) => {
    setSelectedModel(model);
    if (!activeId) return;
    try {
      const { data } = await client.patch(`/chat/conversations/${activeId}`, { model });
      setActive(data);
      await loadConversations();
    } catch (err) {
      setError(errorMessage(err));
    }
  };

  const removeConversation = async (id, event) => {
    event.stopPropagation();
    try {
      await client.delete(`/chat/conversations/${id}`);
      const list = await loadConversations();
      if (activeId === id) {
        setActiveId(null);
        setActive(null);
        if (list.length === 0) setActive(null);
      }
    } catch (err) {
      setError(errorMessage(err));
    }
  };

  const onFileChange = (event) => {
    const next = event.target.files?.[0];
    if (!next) return;
    const okType =
      ALLOWED_TYPES.includes(next.type) ||
      /\.(pdf|png|jpe?g|webp|gif)$/i.test(next.name);
    if (!okType) {
      setError('Supported files: PDF, PNG, JPG, JPEG, WEBP, GIF.');
      event.target.value = '';
      return;
    }
    if (next.size > 10 * 1024 * 1024) {
      setError('File is too large. Maximum size is 10MB.');
      event.target.value = '';
      return;
    }
    setError('');
    setFile(next);
  };

  const sendMessage = async (event) => {
    event?.preventDefault();
    const content = draft.trim();
    if ((!content && !file) || busy) return;

    setBusy(true);
    setError('');
    try {
      let conversationId = activeId;
      if (!conversationId) {
        const created = await client.post('/chat/conversations', { model: selectedModel });
        conversationId = created.data.id;
        setActiveId(conversationId);
        setActive(created.data);
      }

      const preview = file
        ? `📎 ${file.name}\n\n${content || 'Please read this file.'}`
        : content;

      setDraft('');
      const attached = file;
      setFile(null);
      if (fileRef.current) fileRef.current.value = '';

      const optimistic = {
        ...(active || { id: conversationId, title: 'New chat', model: selectedModel, messages: [] }),
        messages: [
          ...((active && active.id === conversationId ? active.messages : []) || []),
          { id: `temp-${Date.now()}`, role: 'user', content: preview, createdAt: new Date().toISOString() },
        ],
      };
      setActive(optimistic);

      let data;
      if (attached) {
        const form = new FormData();
        form.append('content', content);
        form.append('file', attached);
        const response = await client.post(`/chat/conversations/${conversationId}/upload`, form);
        data = response.data;
      } else {
        const response = await client.post(`/chat/conversations/${conversationId}/messages`, { content });
        data = response.data;
      }

      setActive(data);
      setActiveId(data.id);
      await loadConversations();
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
      textareaRef.current?.focus();
    }
  };

  const onKeyDown = (event) => {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      sendMessage();
    }
  };

  const composer = (
    <form className={`chat-composer-pill${showEmpty ? ' chat-composer-pill-hero' : ''}`} onSubmit={sendMessage}>
      <input
        ref={fileRef}
        type="file"
        accept=".pdf,image/png,image/jpeg,image/jpg,image/webp,image/gif,application/pdf"
        hidden
        onChange={onFileChange}
      />
      <button
        type="button"
        className="chat-pill-attach"
        disabled={busy}
        onClick={() => fileRef.current?.click()}
        title="Attach PDF or image"
        aria-label="Attach PDF or image"
      >
        +
      </button>
      <div className="chat-composer-main">
        {file && (
          <div className="chat-file-preview">
            <span>
              {/\.pdf$/i.test(file.name) ? '📄' : '🖼️'} {file.name}
            </span>
            <button
              type="button"
              onClick={() => {
                setFile(null);
                if (fileRef.current) fileRef.current.value = '';
              }}
            >
              ×
            </button>
          </div>
        )}
        <textarea
          ref={textareaRef}
          rows={1}
          placeholder="Ask anything"
          value={draft}
          onChange={(e) => setDraft(e.target.value)}
          onKeyDown={onKeyDown}
          disabled={busy}
        />
      </div>
      <button
        type="submit"
        className="chat-pill-send"
        disabled={busy || (!draft.trim() && !file)}
        aria-label="Send message"
      >
        {busy ? '…' : '↑'}
      </button>
    </form>
  );

  return (
    <div className={`chat-shell${sidebarOpen ? ' sidebar-open' : ''}`}>
      <button
        type="button"
        className="chat-sidebar-backdrop"
        aria-label="Close sidebar"
        onClick={() => setSidebarOpen(false)}
      />

      <aside className="chat-sidebar">
        <div className="chat-sidebar-top">
          <button type="button" className="chat-new-btn" onClick={startNewChat} disabled={busy}>
            <span aria-hidden="true">✎</span> New chat
          </button>
          <nav className="chat-side-nav" aria-label="Quick links">
            <Link className="chat-side-link" to="/generate" onClick={() => setSidebarOpen(false)}>
              <span aria-hidden="true">✦</span> Create draft
            </Link>
            <Link className="chat-side-link" to="/history" onClick={() => setSidebarOpen(false)}>
              <span aria-hidden="true">▤</span> Library
            </Link>
            <Link className="chat-side-link" to="/plagiarism" onClick={() => setSidebarOpen(false)}>
              <span aria-hidden="true">✓</span> Originality
            </Link>
            <Link className="chat-side-link" to="/chapters" onClick={() => setSidebarOpen(false)}>
              <span aria-hidden="true">◉</span> Summarize
            </Link>
          </nav>
        </div>

        <div className="chat-sidebar-list">
          <p className="chat-recents-label">Recents</p>
          {loadingList ? (
            <div className="chat-sidebar-empty">
              <Spinner size="sm" animation="border" />
            </div>
          ) : conversations.length === 0 ? (
            <div className="chat-sidebar-empty">No chats yet</div>
          ) : (
            conversations.map((item) => (
              <button
                type="button"
                key={item.id}
                className={`chat-thread${activeId === item.id ? ' active' : ''}`}
                onClick={() => openConversation(item.id)}
              >
                <span className="chat-thread-title">{item.title}</span>
                <span className="chat-thread-meta">
                  <span>{item.model}</span>
                  <button
                    type="button"
                    className="chat-thread-delete"
                    aria-label="Delete chat"
                    onClick={(event) => removeConversation(item.id, event)}
                  >
                    ×
                  </button>
                </span>
              </button>
            ))
          )}
        </div>

        <div className="chat-sidebar-user">
          <span className="chat-user-avatar" aria-hidden="true">
            {initials(user?.name)}
          </span>
          <div className="chat-user-meta">
            <strong>{user?.name || 'WordWise user'}</strong>
            <span>{user?.subscriptionPlan || 'FREE'}</span>
          </div>
          {user?.subscriptionPlan !== 'PRO' && (
            <Link className="chat-claim-btn" to="/pricing" onClick={() => setSidebarOpen(false)}>
              Upgrade
            </Link>
          )}
        </div>
      </aside>

      <section className="chat-main">
        <header className="chat-toolbar">
          <div className="chat-toolbar-left">
            <button
              type="button"
              className="chat-menu-toggle"
              aria-label="Open sidebar"
              onClick={() => setSidebarOpen(true)}
            >
              ☰
            </button>
            <div className="chat-brand-chip">
              <span>WordWise</span>
              <Form.Select
                className="chat-model-select"
                value={selectedModel}
                onChange={(e) => changeModel(e.target.value)}
                aria-label="Choose AI model"
              >
                {(models.length ? models : [{ id: 'mock', label: 'Demo (offline)', available: true }]).map(
                  (model) => (
                    <option
                      key={model.id}
                      value={model.id}
                      disabled={!model.available && model.id !== selectedModel}
                    >
                      {model.label}
                      {!model.available ? ' (key missing)' : ''}
                    </option>
                  )
                )}
              </Form.Select>
            </div>
          </div>
          {!showEmpty && <p className="chat-active-title">{active?.title}</p>}
        </header>

        <div className={`chat-messages${showEmpty ? ' chat-messages-empty' : ''}`}>
          {loadingChat ? (
            <div className="chat-empty">
              <Spinner animation="border" />
            </div>
          ) : showEmpty ? (
            <div className="chat-empty chat-empty-hero">
              <h2>Ready when you are.</h2>
              <div className="chat-hero-composer">{composer}</div>
              <div className="chat-suggestions">
                {SUGGESTIONS.map((suggestion) => (
                  <button
                    type="button"
                    key={suggestion.label}
                    className="chat-suggestion"
                    onClick={() => {
                      setDraft(suggestion.prompt);
                      textareaRef.current?.focus();
                    }}
                  >
                    {suggestion.label}
                  </button>
                ))}
              </div>
            </div>
          ) : (
            active.messages.map((message) => {
              const parsed = parseMessageContent(message.content);
              return (
                <article
                  key={message.id}
                  className={`chat-bubble-row ${message.role === 'user' ? 'user' : 'assistant'}`}
                >
                  <div className="chat-bubble">
                    <div className="chat-bubble-role">
                      {message.role === 'user' ? 'You' : 'WordWise'}
                    </div>
                    {parsed.attachment && (
                      <div className="chat-attachment-chip">
                        {parsed.attachment.kind === 'PDF' ? '📄' : '🖼️'} {parsed.attachment.name}
                      </div>
                    )}
                    <div className="chat-bubble-text">
                      {message.role === 'assistant' ? (
                        <FormattedContent content={parsed.text} />
                      ) : (
                        parsed.text
                      )}
                    </div>
                    {message.createdAt && (
                      <div className="chat-bubble-time">{formatTime(message.createdAt)}</div>
                    )}
                  </div>
                </article>
              );
            })
          )}
          {busy && !showEmpty && (
            <article className="chat-bubble-row assistant">
              <div className="chat-bubble thinking">
                <div className="chat-bubble-role">WordWise</div>
                <div className="chat-bubble-text">Thinking…</div>
              </div>
            </article>
          )}
          <div ref={bottomRef} />
        </div>

        {error && <div className="chat-error">{error}</div>}

        {!showEmpty && (
          <div className="chat-composer-dock">
            {composer}
            <p className="chat-hint">PDF & images up to 10MB · Enter to send · Shift+Enter for a new line</p>
          </div>
        )}
      </section>
    </div>
  );
}
