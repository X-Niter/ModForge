import React from 'react';
import { useLocation } from 'wouter';

interface NavLinkProps {
  href: string;
  children: React.ReactNode;
  icon?: React.ReactNode;
}

export function NavLink({ href, children, icon }: NavLinkProps) {
  const [location, navigate] = useLocation();
  const isActive = location === href;
  
  const handleClick = (e: React.MouseEvent<HTMLDivElement>) => {
    e.preventDefault();
    navigate(href);
  };
  
  return (
    <div 
      onClick={handleClick}
      className={`sidebar-btn ${isActive ? 'active' : ''} w-full text-left px-4 py-2 flex items-center text-white hover:bg-gray-700 cursor-pointer`}
    >
      {icon && icon}
      <span>{children}</span>
    </div>
  );
}