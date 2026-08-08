import { useCallback, useEffect, useRef, useState } from 'react';
import { Button, Form, Spinner } from 'react-bootstrap';
import client, { errorMessage } from '../api/client';

const formatTime = (value) => (value ? new Date(value).toLocaleString() : '');

export default function Chat() {
  const [conversations, setConversations] = useState([]);
  const [models, setModels] = useState([]);
  const [activeId, setActiveId] = useState(null);
  const [active, setActive] = useState(null);
  const [draft, setDraft] = useState('');
  const [busy, setBusy] = useState(false);
  const [loadingList, setLoadingList] = useState(true);
  const [loadingChat, setLoadingChat] = useState(false);
  const [error, setError] = useState('');
  const [selectedModel, setSelectedModel] = useState('mock');
  const bottomRef = useRef(null);
  const textareaRef = useRef(null);

  const loadConversations = useCallback(async () => {
    const { data } = await client.get('/chat/conversations');
    setConversations(data);
    return data;
  }, []);

  const openConversation = useCallback(async (id) => {
    setLoadingChat(true);
    setError('');
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
        if (list.length) {
          await openConversation(list[0].id);
        }
      } catch (err) {
        if (!cancelled) setError(errorMessage(err));
      } finally {
        if (!cancelled) setLoadingList(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [loadConversations, openConversation]);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth', block: 'end' });
  }, [active?.messages, busy]);

  const startNewChat = async () => {
    setBusy(true);
    setError('');
    try {
      const { data } = await client.post('/chat/conversations', { model: selectedModel });
      await loadConversations();
      setActiveId(data.id);
      setActive(data);
      setDraft('');
      textareaRef.current?.focus();
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
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
        if (list.length) await openConversation(list[0].id);
        else {
          setActiveId(null);
          setActive(null);
        }
      }
    } catch (err) {
      setError(errorMessage(err));
    }
  };

  const sendMessage = async (event) => {
    event?.preventDefault();
    const content = draft.trim();
    if (!content || busy) return;

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

      setDraft('');
      const optimistic = {
        ...(active || { id: conversationId, title: 'New chat', model: selectedModel, messages: [] }),
        messages: [
          ...((active && active.id === conversationId ? active.messages : []) || []),
          { id: `temp-${Date.now()}`, role: 'user', content, createdAt: new Date().toISOString() },
        ],
      };
      setActive(optimistic);

      const { data } = await client.post(`/chat/conversations/${conversationId}/messages`, { content });
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

  return (
    <div className="chat-shell">
      <aside className="chat-sidebar">
        <div className="chat-sidebar-top">
          <Button className="primary-button w-100" onClick={startNewChat} disabled={busy}>
            + New chat
          </Button>
        </div>
        <div className="chat-sidebar-list">
          {loadingList ? (
            <div className="chat-sidebar-empty">
              <Spinner size="sm" animation="border" />
            </div>
          ) : conversations.length === 0 ? (
            <div className="chat-sidebar-empty">No chats yet. Start one.</div>
          ) : (
            conversations.map((item) => (
              <button
                type="button"
                key={item.id}
                className={`chat-thread ${activeId === item.id ? 'active' : ''}`}
                onClick={() => openConversation(item.id)}
              >
                <span className="chat-thread-title">{item.title}</span>
                <span className="chat-thread-meta">
                  {item.model}
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
      </aside>

      <section className="chat-main">
        <header className="chat-toolbar">
          <div>
            <p className="eyebrow">CHAT</p>
            <h1>{active?.title || 'Ask anything'}</h1>
          </div>
          <Form.Select
            className="chat-model-select"
            value={selectedModel}
            onChange={(e) => changeModel(e.target.value)}
            aria-label="Choose AI model"
          >
            {(models.length ? models : [{ id: 'mock', label: 'Demo (offline)', available: true }]).map(
              (model) => (
                <option key={model.id} value={model.id} disabled={!model.available && model.id !== selectedModel}>
                  {model.label}
                  {!model.available ? ' (key missing)' : ''}
                </option>
              )
            )}
          </Form.Select>
        </header>

        <div className="chat-messages">
          {loadingChat ? (
            <div className="chat-empty">
              <Spinner animation="border" />
            </div>
          ) : !active || !active.messages?.length ? (
            <div className="chat-empty">
              <h2>How can I help you today?</h2>
              <p>Ask a question, continue an old chat from the left, or switch models above.</p>
              <div className="chat-suggestions">
                {['Explain photosynthesis simply', 'Give me study tips for exams', 'Draft a short email apology'].map(
                  (suggestion) => (
                    <button
                      type="button"
                      key={suggestion}
                      className="chat-suggestion"
                      onClick={() => {
                        setDraft(suggestion);
                        textareaRef.current?.focus();
                      }}
                    >
                      {suggestion}
                    </button>
                  )
                )}
              </div>
            </div>
          ) : (
            active.messages.map((message) => (
              <article
                key={message.id}
                className={`chat-bubble-row ${message.role === 'user' ? 'user' : 'assistant'}`}
              >
                <div className="chat-bubble">
                  <div className="chat-bubble-role">
                    {message.role === 'user' ? 'You' : 'WordWise'}
                  </div>
                  <div className="chat-bubble-text">{message.content}</div>
                  {message.createdAt && (
                    <div className="chat-bubble-time">{formatTime(message.createdAt)}</div>
                  )}
                </div>
              </article>
            ))
          )}
          {busy && (
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

        <form className="chat-composer" onSubmit={sendMessage}>
          <textarea
            ref={textareaRef}
            rows={1}
            placeholder="Message WordWise…"
            value={draft}
            onChange={(e) => setDraft(e.target.value)}
            onKeyDown={onKeyDown}
            disabled={busy}
          />
          <Button type="submit" className="primary-button" disabled={busy || !draft.trim()}>
            Send
          </Button>
        </form>
        <p className="chat-hint">Enter to send · Shift+Enter for a new line</p>
      </section>
    </div>
  );
}
