
import React from 'react';

export const GoogleFitIcon: React.FC<{ className?: string }> = ({ className = "w-8 h-8" }) => (
  <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" className={className}>
    <path d="M11.87 21.43L6.32 15.88V9.32L11.87 3.77L15.35 7.25L11.87 10.73V21.43Z" fill="#EA4335"/>
    <path d="M17.42 12.58L15.35 14.65L11.87 11.17V3.77L17.42 9.32V12.58Z" fill="#FBBC04"/>
    <path d="M6.32 15.88L2.5 12.06L6.32 8.24V15.88Z" fill="#34A853"/>
    <path d="M15.35 14.65L11.87 18.13L8.39 14.65L11.87 11.17L15.35 14.65Z" fill="#4285F4"/>
  </svg>
);
