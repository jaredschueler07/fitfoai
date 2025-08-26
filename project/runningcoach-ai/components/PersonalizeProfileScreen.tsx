
import React, { useState } from 'react';

interface PersonalizeProfileScreenProps {
  onComplete: (data: { age: number; height: string; weight: string }) => void;
}

const PersonalizeProfileScreen: React.FC<PersonalizeProfileScreenProps> = ({ onComplete }) => {
  const [age, setAge] = useState('');
  const [height, setHeight] = useState('');
  const [weight, setWeight] = useState('');

  const handleSubmit = () => {
    if (age && height && weight) {
      onComplete({ age: parseInt(age), height, weight });
    }
  };

  const isFormValid = age && height && weight;

  const InputField = ({ label, value, onChange, placeholder, type = 'text' }: { label: string, value: string, onChange: (e: React.ChangeEvent<HTMLInputElement>) => void, placeholder: string, type?: string }) => (
    <div>
      <label className="text-neutral-400 mb-2 block">{label}</label>
      <input 
        type={type}
        placeholder={placeholder}
        value={value}
        onChange={onChange}
        className="w-full bg-neutral-800 border border-neutral-700 text-white rounded-xl py-4 px-4 focus:outline-none focus:ring-2 focus:ring-lime-400"
      />
    </div>
  );

  return (
    <div className="w-full h-full bg-black text-white p-6 flex flex-col">
      <div className="pt-16 text-center">
        <h1 className="text-3xl font-bold text-white">Tell us about you</h1>
        <p className="text-neutral-400 mt-2">This helps us create a truly personalized plan. We can pre-fill this from your connected apps.</p>
      </div>

      <div className="flex-1 mt-10 space-y-6">
        <InputField label="Age" value={age} onChange={e => setAge(e.target.value)} placeholder="e.g., 32" type="number" />
        <InputField label="Height" value={height} onChange={e => setHeight(e.target.value)} placeholder="e.g., 5' 10\"" />
        <InputField label="Weight" value={weight} onChange={e => setWeight(e.target.value)} placeholder="e.g., 165 lbs" />
      </div>

      <div className="py-4">
        <button 
          onClick={handleSubmit}
          disabled={!isFormValid}
          className="w-full bg-lime-400 text-black font-bold py-4 px-6 rounded-full flex items-center justify-center text-lg hover:bg-lime-300 transition-colors duration-300 disabled:bg-neutral-600 disabled:cursor-not-allowed"
        >
          Continue
        </button>
      </div>
    </div>
  );
};

export default PersonalizeProfileScreen;
