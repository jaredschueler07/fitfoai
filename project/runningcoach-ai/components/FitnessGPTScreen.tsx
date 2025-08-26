
import React, { useState, useRef, useEffect } from 'react';
import { SparklesIcon } from './icons/SparklesIcon';
import { SendIcon } from './icons/SendIcon';

interface Message {
  text: string;
  sender: 'user' | 'ai';
}

const FitnessGPTScreen: React.FC = () => {
  const [messages, setMessages] = useState<Message[]>([
    { sender: 'ai', text: "Hello! I'm your AI running coach. How can I help you with your training today?" },
    { sender: 'user', text: "I'm feeling a bit sore from my last run. Should I still do my speed workout today?" },
    { sender: 'ai', text: "It's important to listen to your body. Let's talk about how you're feeling. On a scale of 1-5, how would you rate the soreness?" },
  ]);
  const [input, setInput] = useState('');
  const messagesEndRef = useRef<null | HTMLDivElement>(null);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  };

  useEffect(scrollToBottom, [messages]);

  const handleSend = () => {
    if (input.trim()) {
      setMessages([...messages, { text: input, sender: 'user' }]);
      setInput('');
      // Mock AI response
      setTimeout(() => {
        setMessages(prev => [...prev, { text: "That's a great question. Let me think...", sender: 'ai' }]);
      }, 1000);
    }
  };

  const UserMessage: React.FC<{ text: string }> = ({ text }) => (
    <div className="flex justify-end mb-4">
      <div className="bg-lime-400 text-black rounded-l-2xl rounded-tr-2xl p-4 max-w-[80%]">
        <p>{text}</p>
      </div>
    </div>
  );

  const AiMessage: React.FC<{ text: string }> = ({ text }) => (
    <div className="flex justify-start mb-4">
      <div className="bg-neutral-800 text-white rounded-r-2xl rounded-tl-2xl p-4 max-w-[80%]">
        <p>{text}</p>
      </div>
    </div>
  );

  return (
    <div className="w-full h-full bg-black text-white flex flex-col">
      <header className="pt-14 pb-4 px-6 border-b border-neutral-800 flex items-center gap-3">
        <SparklesIcon className="w-7 h-7 text-lime-400" />
        <h1 className="text-2xl font-bold text-white">AI Fitness Coach</h1>
      </header>
      
      <main className="flex-1 p-6 overflow-y-auto">
        {messages.map((msg, index) => (
          msg.sender === 'user' ? <UserMessage key={index} text={msg.text} /> : <AiMessage key={index} text={msg.text} />
        ))}
        <div ref={messagesEndRef} />
      </main>

      <footer className="p-4 bg-black border-t border-neutral-800">
        <div className="flex items-center bg-neutral-900 rounded-full p-2">
          <input
            type="text"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyPress={(e) => e.key === 'Enter' && handleSend()}
            placeholder="Ask your coach anything..."
            className="flex-1 bg-transparent text-white px-4 focus:outline-none placeholder-neutral-500"
          />
          <button 
            onClick={handleSend}
            className="bg-lime-400 text-black rounded-full w-10 h-10 flex items-center justify-center hover:bg-lime-300 transition-colors"
            aria-label="Send message"
          >
            <SendIcon className="w-5 h-5" />
          </button>
        </div>
      </footer>
    </div>
  );
};

export default FitnessGPTScreen;
