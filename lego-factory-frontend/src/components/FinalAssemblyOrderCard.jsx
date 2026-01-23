import React from 'react';
import PropTypes from 'prop-types';
import { Card, Button, Badge } from '../components';
import styles from './FinalAssemblyOrderCard.module.css';

/**
 * FinalAssemblyOrderCard - Card component for Final Assembly Orders (WS-6)
 * 
 * 4-Step Workflow:
 * 1. PENDING → Confirm (verify modules received)
 * 2. CONFIRMED → Start (begin assembly work)
 * 3. IN_PROGRESS → Complete (finish assembly work)
 * 4. COMPLETED_ASSEMBLY → Submit (credit warehouse, activate customer order)
 * 
 * Only SUBMIT credits Plant Warehouse and enables CustomerOrder fulfill button
 */
const FinalAssemblyOrderCard = ({ order, onRefresh }) => {
  const [loading, setLoading] = React.useState(false);
  const [error, setError] = React.useState(null);

  const handleConfirm = async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await fetch(`/api/final-assembly-orders/${order.id}/confirm`, {
        method: 'POST',
        headers: { 
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        }
      });

      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText || 'Failed to confirm order');
      }

      onRefresh();
    } catch (err) {
      console.error('Confirm failed:', err);
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleStart = async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await fetch(`/api/final-assembly-orders/${order.id}/start`, {
        method: 'POST',
        headers: { 
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        }
      });

      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText || 'Failed to start order');
      }

      onRefresh();
    } catch (err) {
      console.error('Start failed:', err);
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleComplete = async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await fetch(`/api/final-assembly-orders/${order.id}/complete`, {
        method: 'POST',
        headers: { 
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        }
      });

      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText || 'Failed to complete assembly');
      }

      onRefresh();
    } catch (err) {
      console.error('Complete failed:', err);
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await fetch(`/api/final-assembly-orders/${order.id}/submit`, {
        method: 'POST',
        headers: { 
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        }
      });

      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText || 'Failed to submit order');
      }

      onRefresh();
    } catch (err) {
      console.error('Submit failed:', err);
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const getStatusBadge = () => {
    const statusColors = {
      'PENDING': 'warning',
      'CONFIRMED': 'info',
      'IN_PROGRESS': 'primary',
      'COMPLETED_ASSEMBLY': 'success',
      'COMPLETED': 'success',
      'HALTED': 'danger',
      'ABANDONED': 'danger'
    };
    return <Badge variant={statusColors[order.status] || 'secondary'}>{order.status}</Badge>;
  };

  const renderActions = () => {
    const actions = [];

    if (order.status === 'PENDING') {
      actions.push(
        <Button 
          key="confirm"
          variant="primary" 
          size="small" 
          onClick={handleConfirm}
          disabled={loading}
        >
          {loading ? 'Confirming...' : 'Confirm'}
        </Button>
      );
    }

    if (order.status === 'CONFIRMED') {
      actions.push(
        <Button 
          key="start"
          variant="success" 
          size="small" 
          onClick={handleStart}
          disabled={loading}
        >
          {loading ? 'Starting...' : 'Start'}
        </Button>
      );
    }

    if (order.status === 'IN_PROGRESS') {
      actions.push(
        <Button 
          key="complete"
          variant="warning" 
          size="small" 
          onClick={handleComplete}
          disabled={loading}
        >
          {loading ? 'Completing...' : 'Complete'}
        </Button>
      );
    }

    if (order.status === 'COMPLETED_ASSEMBLY') {
      actions.push(
        <Button 
          key="submit"
          variant="primary" 
          size="small" 
          onClick={handleSubmit}
          disabled={loading}
        >
          {loading ? 'Submitting...' : 'Submit'}
        </Button>
      );
    }

    return actions;
  };

  return (
    <Card className={styles.card}>
      <div className={styles.header}>
        <div className={styles.orderInfo}>
          <span className={styles.orderNumber}>📦 {order.orderNumber}</span>
          {getStatusBadge()}
        </div>
        {order.priority && (
          <Badge variant={order.priority === 'URGENT' ? 'danger' : 'secondary'}>
            {order.priority}
          </Badge>
        )}
      </div>

      <div className={styles.content}>
        <div className={styles.productInfo}>
          <strong>Product:</strong> {order.outputProductVariantName}
          <br />
          <strong>Quantity:</strong> {order.quantity}
        </div>

        {order.requiredModuleDetails && (
          <div className={styles.moduleInfo}>
            <strong>Required Modules:</strong>
            <div className={styles.moduleList}>
              {JSON.parse(order.requiredModuleDetails).map((module, idx) => (
                <div key={idx} className={styles.moduleItem}>
                  {module.name} (x{module.quantity})
                </div>
              ))}
            </div>
          </div>
        )}

        <div className={styles.timing}>
          {order.actualStartTime && (
            <div><strong>Started:</strong> {new Date(order.actualStartTime).toLocaleString()}</div>
          )}
          {order.actualFinishTime && (
            <div><strong>Finished:</strong> {new Date(order.actualFinishTime).toLocaleString()}</div>
          )}
        </div>

        {order.assemblyInstructions && (
          <div className={styles.instructions}>
            <strong>Instructions:</strong> {order.assemblyInstructions}
          </div>
        )}
      </div>

      {error && (
        <div className={styles.error}>
          ⚠️ {error}
        </div>
      )}

      {renderActions().length > 0 && (
        <div className={styles.actions}>
          {renderActions()}
        </div>
      )}

      {order.status === 'COMPLETED' && (
        <div className={styles.completedInfo}>
          ✅ Order completed and submitted - Products credited to Plant Warehouse
        </div>
      )}
    </Card>
  );
};

FinalAssemblyOrderCard.propTypes = {
  order: PropTypes.shape({
    id: PropTypes.number.isRequired,
    orderNumber: PropTypes.string.isRequired,
    status: PropTypes.string.isRequired,
    outputProductVariantName: PropTypes.string.isRequired,
    quantity: PropTypes.number.isRequired,
    priority: PropTypes.string,
    requiredModuleDetails: PropTypes.string,
    assemblyInstructions: PropTypes.string,
    actualStartTime: PropTypes.string,
    actualFinishTime: PropTypes.string,
  }).isRequired,
  onRefresh: PropTypes.func.isRequired
};

export default FinalAssemblyOrderCard;
