import React, { useCallback, useState } from 'react';
import ReactFlow, {
  Node,
  Edge,
  Controls,
  Background,
  useNodesState,
  useEdgesState,
  addEdge,
  Connection,
  Panel,
} from 'reactflow';
import 'reactflow/dist/style.css';

interface GraphNode {
  id: number;
  label: string;
  xPosition: number;
  yPosition: number;
  color: string;
  shape: string;
  size: number;
  version: number;
}

interface GraphEdge {
  id: number;
  sourceNode: GraphNode;
  targetNode: GraphNode;
  label: string;
  type: string;
  weight: number;
}

interface GraphVisualizationProps {
  nodes: GraphNode[];
  edges: GraphEdge[];
  onNodePositionChange?: (nodeId: number, x: number, y: number) => void;
}

const GraphVisualization: React.FC<GraphVisualizationProps> = ({
  nodes,
  edges,
  onNodePositionChange,
}) => {
  // Convert backend nodes to ReactFlow nodes
  const initialNodes: Node[] = nodes.map((node) => ({
    id: node.id.toString(),
    data: { label: node.label },
    position: { x: node.xPosition, y: node.yPosition },
    style: {
      background: node.color,
      width: node.size,
      height: node.size,
      borderRadius: node.shape === 'circle' ? '50%' : '0%',
      border: '1px solid #ccc',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      color: '#fff',
      fontWeight: 'bold',
    },
  }));

  // Convert backend edges to ReactFlow edges
  const initialEdges: Edge[] = edges.map((edge) => ({
    id: edge.id.toString(),
    source: edge.sourceNode.id.toString(),
    target: edge.targetNode.id.toString(),
    label: edge.label,
    type: edge.type,
    style: { stroke: '#555' },
    animated: false,
  }));

  const [reactFlowNodes, setNodes, onNodesChange] = useNodesState(initialNodes);
  const [reactFlowEdges, setEdges, onEdgesChange] = useEdgesState(initialEdges);

  const onConnect = useCallback(
    (params: Connection) => setEdges((eds) => addEdge(params, eds)),
    [setEdges]
  );

  const onNodeDragStop = useCallback(
    (event: React.MouseEvent, node: Node) => {
      if (onNodePositionChange) {
        onNodePositionChange(
          parseInt(node.id),
          node.position.x,
          node.position.y
        );
      }
    },
    [onNodePositionChange]
  );

  return (
    <div style={{ width: '100%', height: '600px' }}>
      <ReactFlow
        nodes={reactFlowNodes}
        edges={reactFlowEdges}
        onNodesChange={onNodesChange}
        onEdgesChange={onEdgesChange}
        onConnect={onConnect}
        onNodeDragStop={onNodeDragStop}
        fitView
      >
        <Background />
        <Controls />
        <Panel position="top-right">
          <div className="bg-white p-2 rounded shadow">
            <p className="text-sm text-gray-600">
              Drag nodes to move them around
            </p>
          </div>
        </Panel>
      </ReactFlow>
    </div>
  );
};

export default GraphVisualization; 