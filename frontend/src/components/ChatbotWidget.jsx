import { useEffect, useMemo, useRef, useState } from 'react';
import { chatbotService } from '../api/chatbotService';

const STORAGE_KEY = 'mediatron_chatbot_history_v1';
const SIZE_STORAGE_KEY = 'mediatron_chatbot_manual_size_v1';
const MAX_HISTORY = 10;

const DEFAULT_SIZE = {
  width: 520,
  height: 720,
};

const MIN_SIZE = {
  width: 360,
  height: 480,
};

function getMaxSize() {
  if (typeof window === 'undefined') {
    return {
      width: 900,
      height: 820,
    };
  }

  return {
    width: Math.max(MIN_SIZE.width, window.innerWidth - 48),
    height: Math.max(MIN_SIZE.height, window.innerHeight - 72),
  };
}

function clampSize(size) {
  const maxSize = getMaxSize();

  return {
    width: Math.min(Math.max(size.width, MIN_SIZE.width), maxSize.width),
    height: Math.min(Math.max(size.height, MIN_SIZE.height), maxSize.height),
  };
}

const starterSuggestions = [];

function loadStoredMessages() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    const parsed = raw ? JSON.parse(raw) : [];

    if (!Array.isArray(parsed)) {
      return [];
    }

    return parsed
      .filter((item) => item && typeof item.content === 'string')
      .slice(-MAX_HISTORY);
  } catch {
    return [];
  }
}

function saveStoredMessages(messages) {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(messages.slice(-MAX_HISTORY)));
  } catch {
    // Local storage may be unavailable.
  }
}

function loadStoredSize() {
  try {
    const raw = localStorage.getItem(SIZE_STORAGE_KEY);
    const parsed = raw ? JSON.parse(raw) : null;

    if (!parsed || typeof parsed.width !== 'number' || typeof parsed.height !== 'number') {
      return DEFAULT_SIZE;
    }

    return clampSize(parsed);
  } catch {
    return DEFAULT_SIZE;
  }
}

function saveStoredSize(size) {
  try {
    localStorage.setItem(SIZE_STORAGE_KEY, JSON.stringify(size));
  } catch {
    // Local storage may be unavailable.
  }
}

function buildMessage(role, content) {
  return {
    id: `${Date.now()}-${Math.random().toString(16).slice(2)}`,
    role,
    content,
    createdAt: new Date().toISOString(),
  };
}

function renderMessageContent(content) {
  const text = content || '';
  const urlRegex = /(https?:\/\/[^\s)]+)|(www\.[^\s)]+)/g;
  const parts = text.split(urlRegex).filter((part) => part !== undefined && part !== '');

  return parts.map((part, index) => {
    const isUrl = part.startsWith('http://') || part.startsWith('https://') || part.startsWith('www.');
    const href = part.startsWith('www.') ? `https://${part}` : part;

    if (!isUrl) {
      return <span key={`${part}-${index}`}>{part}</span>;
    }

    return (
      <a
        key={`${part}-${index}`}
        href={href}
        target="_blank"
        rel="noreferrer"
        className="font-bold text-[#b8892f] underline decoration-[#e8cd90] underline-offset-4 hover:text-[#1b2d52]"
      >
        {part}
      </a>
    );
  });
}

export default function ChatbotWidget() {
  const [open, setOpen] = useState(false);
  const [messages, setMessages] = useState(() => loadStoredMessages());
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [suggestions, setSuggestions] = useState(starterSuggestions);
  const [size, setSize] = useState(() => loadStoredSize());

  const resizeRef = useRef(null);
  const messagesEndRef = useRef(null);

  const visibleMessages = useMemo(() => messages.slice(-MAX_HISTORY), [messages]);

  useEffect(() => {
    saveStoredMessages(messages);
  }, [messages]);

  useEffect(() => {
    saveStoredSize(size);
  }, [size]);

  useEffect(() => {
    if (open) {
      messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    }
  }, [open, messages, loading]);

  useEffect(() => {
    const handleResize = () => {
      setSize((current) => clampSize(current));
    };

    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  useEffect(() => {
    const handlePointerMove = (event) => {
      if (!resizeRef.current) {
        return;
      }

      const { startX, startY, startWidth, startHeight } = resizeRef.current;

      const nextSize = clampSize({
        width: startWidth + (startX - event.clientX),
        height: startHeight + (startY - event.clientY),
      });

      setSize(nextSize);
    };

    const stopResize = () => {
      resizeRef.current = null;
      document.body.style.userSelect = '';
      document.body.style.cursor = '';
    };

    window.addEventListener('pointermove', handlePointerMove);
    window.addEventListener('pointerup', stopResize);
    window.addEventListener('pointercancel', stopResize);

    return () => {
      window.removeEventListener('pointermove', handlePointerMove);
      window.removeEventListener('pointerup', stopResize);
      window.removeEventListener('pointercancel', stopResize);
    };
  }, []);

  const startResize = (event) => {
    event.preventDefault();
    event.stopPropagation();

    resizeRef.current = {
      startX: event.clientX,
      startY: event.clientY,
      startWidth: size.width,
      startHeight: size.height,
    };

    document.body.style.userSelect = 'none';
    document.body.style.cursor = 'nwse-resize';
  };

  const sendMessage = async (value) => {
    const cleaned = value.trim();

    if (!cleaned || loading) {
      return;
    }

    const userMessage = buildMessage('user', cleaned);
    const nextMessages = [...messages, userMessage].slice(-MAX_HISTORY);

    setMessages(nextMessages);
    setInput('');
    setError('');
    setLoading(true);

    try {
      const response = await chatbotService.sendMessage(cleaned, nextMessages);
      const data = response.data || {};
      const answer = data.answer || 'I could not generate an answer right now.';
      const assistantMessage = buildMessage('assistant', answer);

      setMessages((current) => [...current, assistantMessage].slice(-MAX_HISTORY));

    } catch (err) {
      const data = err?.response?.data;
      const message =
        data?.message ||
        data?.error ||
        (typeof data === 'string' && data) ||
        err?.message ||
        'The chatbot could not respond. Please try again.';

      setError(message);
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = (event) => {
    event.preventDefault();
    sendMessage(input).catch(() => {});
  };

  const clearConversation = () => {
    setMessages([]);
    setError('');
    localStorage.removeItem(STORAGE_KEY);
  };

  const resetSize = () => {
    setSize(DEFAULT_SIZE);
  };

  return (
    <div className="fixed bottom-5 right-5 z-[9999] font-sans text-[#1b2d52]">
      {open && (
        <section
          className="relative mb-4 flex max-h-[calc(100vh-3.5rem)] max-w-[calc(100vw-2.5rem)] flex-col overflow-hidden rounded-[2rem] border border-[#e3dac9] bg-[#fffdf8] shadow-2xl"
          style={{
            width: `${size.width}px`,
            height: `${size.height}px`,
          }}
        >
          <button
            type="button"
            onPointerDown={startResize}
            className="absolute left-0 top-0 z-20 h-9 w-9 cursor-nwse-resize rounded-br-2xl bg-[#e8cd90] text-[#1b2d52] shadow-md transition hover:bg-[#f3ddaa]"
            title="Drag to resize chatbot"
            aria-label="Drag to resize chatbot"
          >
            ↖
          </button>

          <header className="shrink-0 bg-[#1b2d52] px-5 py-4 pl-12 text-white">
            <div className="flex items-start justify-between gap-4">
              <div className="min-w-0">
                <p className="text-[10px] font-black uppercase tracking-[0.28em] text-[#e8cd90]">
                  MEDIATRON Chatbot
                </p>
                <h2 className="mt-1 font-serif text-xl">Research Assistant</h2>
                <p className="mt-1 text-xs leading-5 text-white/70">
                  Ask about publications, members, projects, events, or request explanations from publication source links.
                </p>
              </div>

              <div className="flex shrink-0 items-center gap-2">
                <button
                  type="button"
                  onClick={resetSize}
                  className="rounded-full border border-white/20 px-3 py-1 text-xs font-bold uppercase tracking-[0.14em] text-white/80 transition hover:bg-white/10"
                  title="Reset chatbot size"
                >
                  Reset
                </button>

                <button
                  type="button"
                  onClick={() => setOpen(false)}
                  className="rounded-full border border-white/20 px-3 py-1 text-xs font-bold uppercase tracking-[0.14em] text-white/80 transition hover:bg-white/10"
                >
                  Close
                </button>
              </div>
            </div>
          </header>

          <div className="min-h-0 flex-1 space-y-4 overflow-y-auto px-4 py-4">
            {visibleMessages.length === 0 && (
              <div className="rounded-[1.5rem] border border-[#eadfc9] bg-white p-4 shadow-sm">
                <p className="font-serif text-lg text-[#1b2d52]">Hello! How can I help?</p>
                <p className="mt-2 text-sm leading-6 text-[#637390]">
                  I can search MEDIATRON publications, members, lab events, research projects, and explain accessible publication sources.
                </p>
              </div>
            )}

            {visibleMessages.map((message) => {
              const isUser = message.role === 'user';

              return (
                <div
                  key={message.id || `${message.role}-${message.createdAt}`}
                  className={`flex ${isUser ? 'justify-end' : 'justify-start'}`}
                >
                  <div
                    className={`max-w-[88%] whitespace-pre-wrap rounded-[1.4rem] px-4 py-3 text-sm leading-6 shadow-sm ${
                      isUser
                        ? 'bg-[#1b2d52] text-white'
                        : 'border border-[#e9decf] bg-white text-[#344362]'
                    }`}
                  >
                    {renderMessageContent(message.content)}
                  </div>
                </div>
              );
            })}

            {loading && (
              <div className="flex justify-start">
                <div className="rounded-[1.4rem] border border-[#e9decf] bg-white px-4 py-3 text-sm text-[#637390] shadow-sm">
                  Reading MEDIATRON data...
                </div>
              </div>
            )}

            {error && (
              <div className="rounded-[1.4rem] border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">
                {error}
              </div>
            )}

            <div ref={messagesEndRef} />
          </div>

          <div className="shrink-0 border-t border-[#eadfc9] bg-white px-4 py-3">

            <form onSubmit={handleSubmit} className="flex items-end gap-2">
              <textarea
                value={input}
                onChange={(event) => setInput(event.target.value)}
                onKeyDown={(event) => {
                  if (event.key === 'Enter' && !event.shiftKey) {
                    event.preventDefault();
                    sendMessage(input).catch(() => {});
                  }
                }}
                placeholder="Ask the MEDIATRON Chatbot..."
                rows={2}
                className="min-h-[52px] flex-1 resize-none rounded-[1.2rem] border border-[#d8dfeb] bg-[#fbfcfe] px-4 py-3 text-sm outline-none transition focus:border-[#c8a864]"
              />

              <button
                type="submit"
                disabled={loading || !input.trim()}
                className="rounded-[1.2rem] bg-[#c8a864] px-4 py-3 text-sm font-black uppercase tracking-[0.14em] text-[#1b2d52] transition hover:bg-[#d8ba75] disabled:cursor-not-allowed disabled:opacity-50"
              >
                Send
              </button>
            </form>

            {messages.length > 0 && (
              <button
                type="button"
                onClick={clearConversation}
                className="mt-2 text-xs font-bold uppercase tracking-[0.14em] text-[#8d9ab3] hover:text-[#1b2d52]"
              >
                Clear conversation
              </button>
            )}
          </div>
        </section>
      )}

      <button
        type="button"
        onClick={() => setOpen((current) => !current)}
        className="ml-auto flex h-16 min-w-16 items-center justify-center rounded-full bg-[#1b2d52] px-5 text-sm font-black uppercase tracking-[0.16em] text-white shadow-2xl ring-4 ring-[#e8cd90]/30 transition hover:scale-105 hover:bg-[#263f73]"
        aria-label="Open MEDIATRON chatbot"
      >
        {open ? '×' : 'Chatbot'}
      </button>
    </div>
  );
}