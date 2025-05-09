import * as React from "react";
import { cn } from "@/lib/utils";

type ProgressCircleProps = {
  value: number;
  size?: "sm" | "md" | "lg";
  className?: string;
};

export function ProgressCircle({
  value,
  size = "md",
  className,
  ...props
}: ProgressCircleProps & React.HTMLAttributes<SVGSVGElement>) {
  // Ensure value is between 0 and 100
  const clampedValue = Math.max(0, Math.min(100, value));
  
  // Calculate the circle properties
  const radius = size === "sm" ? 7 : size === "md" ? 9 : 11;
  const circumference = 2 * Math.PI * radius;
  const strokeDasharray = circumference;
  const strokeDashoffset = circumference - (clampedValue / 100) * circumference;
  
  // Size of the SVG viewBox
  const viewBoxSize = (radius + 2) * 2;
  const center = viewBoxSize / 2;
  
  return (
    <svg
      viewBox={`0 0 ${viewBoxSize} ${viewBoxSize}`}
      className={cn(
        "rotate-[-90deg]",
        size === "sm" ? "h-4 w-4" : size === "md" ? "h-10 w-10" : "h-12 w-12",
        className
      )}
      {...props}
    >
      {/* Background circle */}
      <circle
        cx={center}
        cy={center}
        r={radius}
        className="stroke-gray-200 dark:stroke-gray-700"
        strokeWidth="2"
        fill="none"
      />
      
      {/* Progress circle */}
      <circle
        cx={center}
        cy={center}
        r={radius}
        className={cn("stroke-current transition-all duration-300 ease-in-out", className)}
        strokeWidth="2"
        strokeLinecap="round"
        strokeDasharray={strokeDasharray}
        strokeDashoffset={strokeDashoffset}
        fill="none"
      />
    </svg>
  );
}