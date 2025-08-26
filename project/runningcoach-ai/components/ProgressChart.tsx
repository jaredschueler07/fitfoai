import React from 'react';

interface ChartData {
  label: string;
  value: number;
}

interface ProgressChartProps {
  data: ChartData[];
}

const ProgressChart: React.FC<ProgressChartProps> = ({ data }) => {
  const maxValue = Math.max(...data.map(d => d.value), 1); // Ensure maxValue is at least 1 to avoid division by zero
  const chartHeight = 120; // in px
  const barWidth = 24;
  const barMargin = 16;
  const svgWidth = data.length * (barWidth + barMargin) - barMargin;

  return (
    <div className="bg-neutral-900 border border-neutral-800 rounded-2xl p-4">
      <svg viewBox={`0 0 ${svgWidth} ${chartHeight + 20}`} className="w-full h-auto" aria-labelledby="chart-title">
        <title id="chart-title">Weekly workout progress chart</title>
        {data.map((item, index) => {
          const barHeight = (item.value / maxValue) * chartHeight;
          const x = index * (barWidth + barMargin);
          const y = chartHeight - barHeight;

          return (
            <g key={index} className="group">
              <rect
                x={x}
                y={y}
                width={barWidth}
                height={barHeight}
                rx="4"
                className="fill-lime-400/50 group-hover:fill-lime-400 transition-colors"
              />
              <text
                x={x + barWidth / 2}
                y={chartHeight + 15}
                textAnchor="middle"
                className="fill-neutral-400 text-xs font-mono"
              >
                {item.label}
              </text>
            </g>
          );
        })}
      </svg>
    </div>
  );
};

export default ProgressChart;
