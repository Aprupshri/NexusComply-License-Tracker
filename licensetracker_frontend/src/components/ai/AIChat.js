import React, { useState, useEffect, useRef } from 'react';
import {
    Container, Row, Col, Card, Form, Button, Badge, Spinner,
    ListGroup, Alert, Tooltip, OverlayTrigger
} from 'react-bootstrap';
import { useNavigate } from 'react-router-dom';
import { toast } from 'sonner';
import ReactMarkdown from 'react-markdown';
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';
import { dracula } from 'react-syntax-highlighter/dist/esm/styles/prism';
import NavigationBar from '../common/Navbar';
import Sidebar from '../common/Sidebar';
import chatService from '../../services/chatService';
import './AIChat.css';

const AIChat = () => {
    const [messages, setMessages] = useState([]);
    const [input, setInput] = useState('');
    const [loading, setLoading] = useState(false);
    const [chatId, setChatId] = useState(null);
    const [showSuggestions, setShowSuggestions] = useState(true);
    const messagesEndRef = useRef(null);
    const navigate = useNavigate();

    // AI Suggestions/Prompts
    const suggestions = [
        {
            category: 'License Management',
            prompts: [
                {
                    icon: 'bi-exclamation-triangle',
                    title: 'Expiring Licenses',
                    prompt: 'Show me all licenses expiring in the next 30 days'
                },
                {
                    icon: 'bi-bar-chart',
                    title: 'License Summary',
                    prompt: 'Give me a summary of all licenses including active, expiring, and expired'
                },
                {
                    icon: 'bi-graph-up',
                    title: 'Usage Analysis',
                    prompt: 'Analyze license utilization and capacity'
                },
                {
                    icon: 'bi-currency-rupee',
                    title: 'Renewal Forecast',
                    prompt: 'What will be the total renewal cost for licenses expiring in next 90 days?'
                }
            ]
        },
        {
            category: 'Device Management',
            prompts: [
                {
                    icon: 'bi-hdd-network',
                    title: 'Device Summary',
                    prompt: 'Show me a summary of all devices by lifecycle status'
                },
                {
                    icon: 'bi-tools',
                    title: 'Software Updates',
                    prompt: 'List devices with outdated software versions'
                },
                {
                    icon: 'bi-arrow-repeat',
                    title: 'Update Recommendations',
                    prompt: 'What software updates are recommended for our devices?'
                }
            ]
        },
        {
            category: 'Compliance & Reporting',
            prompts: [
                {
                    icon: 'bi-clipboard-check',
                    title: 'Compliance Status',
                    prompt: 'Give me a compliance summary with all license statuses'
                },
                {
                    icon: 'bi-book',
                    title: 'Training Guide',
                    prompt: 'Create a training checklist for new users'
                },
                {
                    icon: 'bi-award',
                    title: 'Best Practices',
                    prompt: 'What are the best practices for license management?'
                }
            ]
        }
    ];

    useEffect(() => {
        scrollToBottom();
        initializeChat();
    }, []);

    useEffect(() => {
        scrollToBottom();
    }, [messages]);

    const initializeChat = () => {
        const username = JSON.parse(localStorage.getItem('user'))?.username || 'unknown';
        const id = `${username}-${Date.now()}`;
        setChatId(id);

        const welcomeMessage = {
            id: 1,
            text: '👋 Welcome to NexusComply AI Assistant! I\'m here to help you with license management, device tracking, and compliance reporting. Ask me anything about your licenses, devices, or get insights for better decision-making.',
            sender: 'ai',
            timestamp: new Date()
        };
        setMessages([welcomeMessage]);
    };

    const scrollToBottom = () => {
        messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    };

    const handleSendMessage = async (e) => {
        e.preventDefault();

        if (!input.trim()) {
            toast.error('Please enter a message');
            return;
        }

        const userMessage = {
            id: messages.length + 1,
            text: input,
            sender: 'user',
            timestamp: new Date()
        };

        setMessages(prev => [...prev, userMessage]);
        setInput('');
        setShowSuggestions(false);
        setLoading(true);

        try {
            const response = await chatService.sendMessage(input);

            const aiMessage = {
                id: messages.length + 2,
                text: response.response,
                sender: 'ai',
                timestamp: new Date(),
                chatId: response.chatId
            };

            setMessages(prev => [...prev, aiMessage]);
            if (response.chatId) {
                setChatId(response.chatId);
            }
        } catch (error) {
            console.error('Error sending message:', error);
            const errorMessage = {
                id: messages.length + 2,
                text: 'Sorry, I encountered an error. Please try again.',
                sender: 'ai',
                timestamp: new Date(),
                isError: true
            };
            setMessages(prev => [...prev, errorMessage]);
            toast.error('Failed to send message');
        } finally {
            setLoading(false);
        }
    };

    const handleSuggestionClick = (prompt) => {
        setInput(prompt);
    };

    const handleClearChat = () => {
        if (window.confirm('Are you sure you want to clear the chat history?')) {
            setMessages([
                {
                    id: 1,
                    text: '👋 Welcome to NexusComply AI Assistant! I\'m here to help you with license management, device tracking, and compliance reporting.',
                    sender: 'ai',
                    timestamp: new Date()
                }
            ]);
            initializeChat();
            toast.success('Chat cleared');
        }
    };

    const copyToClipboard = (text) => {
        navigator.clipboard.writeText(text);
        toast.success('Copied to clipboard');
    };

    // Custom renderers for react-markdown
    const markdownComponents = {
        code({ node, inline, className, children, ...props }) {
            const match = /language-(\w+)/.exec(className || '');
            const language = match ? match[1] : '';

            if (!inline && language) {
                return (
                    <SyntaxHighlighter
                        style={dracula}
                        language={language}
                        PreTag="div"
                        className="rounded"
                        {...props}
                    >
                        {String(children).replace(/\n$/, '')}
                    </SyntaxHighlighter>
                );
            }

            return (
                <code className="bg-light px-2 py-1 rounded" {...props}>
                    {children}
                </code>
            );
        },
        table({ node, children, ...props }) {
            return (
                <table className="table table-sm table-bordered mt-2 mb-2" {...props}>
                    {children}
                </table>
            );
        },
        ul({ node, children, ...props }) {
            return (
                <ul className="ms-3" {...props}>
                    {children}
                </ul>
            );
        },
        ol({ node, children, ...props }) {
            return (
                <ol className="ms-3" {...props}>
                    {children}
                </ol>
            );
        },
        blockquote({ node, children, ...props }) {
            return (
                <blockquote className="border-start border-primary ps-3 ms-2" {...props}>
                    {children}
                </blockquote>
            );
        }
    };

    const renderMessage = (msg) => {
        return (
            <div
                key={msg.id}
                className={`message-container ${msg.sender === 'user' ? 'user-message' : 'ai-message'}`}
            >
                <div className={`message ${msg.sender === 'user' ? 'user' : 'ai'} ${msg.isError ? 'error' : ''}`}>
                    {msg.sender === 'ai' && (
                        <div className="ai-avatar">
                            <i className="bi bi-robot"></i>
                        </div>
                    )}
                    <div className="message-content">
                        <div className="message-text">
                            {msg.sender === 'ai' ? (
                                <ReactMarkdown components={markdownComponents}>
                                    {msg.text}
                                </ReactMarkdown>
                            ) : (
                                msg.text.split('\n').map((line, idx) => (
                                    <React.Fragment key={idx}>
                                        {line}
                                        <br />
                                    </React.Fragment>
                                ))
                            )}
                        </div>
                        <div className="message-footer">
                            <small className="text-muted">
                                {new Date(msg.timestamp).toLocaleTimeString([], {
                                    hour: '2-digit',
                                    minute: '2-digit'
                                })}
                            </small>
                            {msg.sender === 'ai' && (
                                <OverlayTrigger
                                    placement="top"
                                    overlay={<Tooltip>Copy response</Tooltip>}
                                >
                                    <Button
                                        variant="link"
                                        size="sm"
                                        className="ms-2 copy-btn"
                                        onClick={() => copyToClipboard(msg.text)}
                                    >
                                        <i className="bi bi-clipboard"></i>
                                    </Button>
                                </OverlayTrigger>
                            )}
                        </div>
                    </div>
                </div>
            </div>
        );
    };

    return (
        <>
            <NavigationBar />
            <div className="d-flex">
                <Sidebar />
                <div className="ai-chat-container flex-grow-1 p-2 p-md-4">
                    <Container fluid className="px-2 px-md-0 h-100">
                        <Row className="h-100 g-0">
                            {/* Chat Area */}
                            <Col xs={12} lg={9} className="d-flex flex-column">
                                {/* Header */}
                                <Card className="border-0 shadow-sm mb-3">
                                    <Card.Body className="d-flex align-items-center justify-content-between p-3">
                                        <div className="d-flex align-items-center gap-2">
                                            <div className="ai-header-avatar">
                                                <i className="bi bi-robot"></i>
                                            </div>
                                            <div>
                                                <h5 className="mb-0">
                                                    <i className="bi bi-sparkles me-2"></i>
                                                    NexusComply AI Assistant
                                                </h5>
                                                <small className="text-muted">Always here to help</small>
                                            </div>
                                        </div>
                                        <Button
                                            variant="outline-danger"
                                            size="sm"
                                            onClick={handleClearChat}
                                            className="d-none d-md-block"
                                        >
                                            <i className="bi bi-trash me-1"></i>
                                            Clear Chat
                                        </Button>
                                    </Card.Body>
                                </Card>

                                {/* Messages Area */}
                                <Card className="flex-grow-1 shadow-sm border-0 messages-card">
                                    <Card.Body className="messages-area overflow-auto p-3">
                                        {messages.length === 0 ? (
                                            <div className="text-center text-muted py-5">
                                                <i className="bi bi-chat-left" style={{ fontSize: '3rem', opacity: 0.3 }}></i>
                                                <p className="mt-3">No messages yet. Start by asking a question!</p>
                                            </div>
                                        ) : (
                                            messages.map(msg => renderMessage(msg))
                                        )}

                                        {loading && (
                                            <div className="message-container ai-message">
                                                <div className="message ai">
                                                    <div className="ai-avatar">
                                                        <i className="bi bi-robot"></i>
                                                    </div>
                                                    <div className="message-content">
                                                        <div className="typing-indicator">
                                                            <span></span>
                                                            <span></span>
                                                            <span></span>
                                                        </div>
                                                    </div>
                                                </div>
                                            </div>
                                        )}

                                        <div ref={messagesEndRef} />
                                    </Card.Body>
                                </Card>

                                {/* Input Area */}
                                <Card className="border-0 shadow-sm mt-3">
                                    <Card.Body className="p-3">
                                        <Form onSubmit={handleSendMessage}>
                                            <div className="input-group input-group-lg">
                                                <Form.Control
                                                    as="textarea"
                                                    rows={3}
                                                    placeholder="Ask me about licenses, devices, compliance... (Shift+Enter for new line, Enter to send)"
                                                    value={input}
                                                    onChange={(e) => setInput(e.target.value)}
                                                    onKeyPress={(e) => {
                                                        if (e.key === 'Enter' && !e.shiftKey) {
                                                            e.preventDefault();
                                                            handleSendMessage(e);
                                                        }
                                                    }}
                                                    disabled={loading}
                                                    className="message-input"
                                                />
                                            </div>
                                            <div className="d-flex gap-2 mt-2">
                                                <Button
                                                    variant="primary"
                                                    type="submit"
                                                    disabled={loading || !input.trim()}
                                                    className="w-100"
                                                >
                                                    {loading ? (
                                                        <>
                                                            <Spinner
                                                                as="span"
                                                                animation="border"
                                                                size="sm"
                                                                className="me-2"
                                                            />
                                                            Sending...
                                                        </>
                                                    ) : (
                                                        <>
                                                            <i className="bi bi-send me-2"></i>
                                                            Send
                                                        </>
                                                    )}
                                                </Button>
                                                <Button
                                                    variant="outline-danger"
                                                    size="sm"
                                                    onClick={handleClearChat}
                                                    className="d-block d-md-none"
                                                >
                                                    <i className="bi bi-trash"></i>
                                                </Button>
                                            </div>
                                        </Form>
                                    </Card.Body>
                                </Card>
                            </Col>

                            {/* Suggestions Sidebar */}
                            <Col xs={12} lg={3} className="ps-lg-3 mt-3 mt-lg-0">
                                {showSuggestions && (
                                    <div className="suggestions-container">
                                        <h6 className="fw-bold mb-3">
                                            <i className="bi bi-lightning-charge me-2"></i>
                                            Quick Prompts
                                        </h6>

                                        {suggestions.map((category, idx) => (
                                            <div key={idx} className="suggestion-category mb-4">
                                                <h6 className="text-muted small mb-2">
                                                    {category.category}
                                                </h6>
                                                <div className="d-flex flex-column gap-2">
                                                    {category.prompts.map((prompt, pidx) => (
                                                        <Button
                                                            key={pidx}
                                                            variant="light"
                                                            size="sm"
                                                            onClick={() => handleSuggestionClick(prompt.prompt)}
                                                            className="suggestion-btn text-start"
                                                            disabled={loading}
                                                        >
                                                            <i className={`bi ${prompt.icon} me-2`}></i>
                                                            <span className="suggestion-text">
                                                                {prompt.title}
                                                            </span>
                                                        </Button>
                                                    ))}
                                                </div>
                                            </div>
                                        ))}

                                        <Alert variant="info" className="small border-0">
                                            <i className="bi bi-info-circle me-2"></i>
                                            <strong>Tip:</strong> Click on any suggestion to fill it in the chat box
                                        </Alert>
                                    </div>
                                )}

                                {!showSuggestions && (
                                    <div className="text-center text-muted py-5">
                                        <Button
                                            variant="outline-primary"
                                            size="sm"
                                            onClick={() => setShowSuggestions(true)}
                                        >
                                            <i className="bi bi-lightning-charge me-1"></i>
                                            Show Suggestions
                                        </Button>
                                    </div>
                                )}
                            </Col>
                        </Row>
                    </Container>
                </div>
            </div>
        </>
    );
};

export default AIChat;
