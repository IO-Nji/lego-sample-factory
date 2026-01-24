import { useState } from 'prop-types';
import PropTypes from 'prop-types';
import Button from './Button';
import './Modal.css';
import '../styles/Modal.css';

/**
 * Modal for requesting parts from Parts Supply Warehouse.
 * Used by Production Control and Assembly Control.
 */
function RequestPartsModal({ show, onClose, controlOrder, controlOrderType, onSuccess }) {
  const [parts, setParts] = useState([{ partId: '', quantity: 1 }]);
  const [neededBy, setNeededBy] = useState('');
  const [notes, setNotes] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  if (!show) return null;

  const handleAddPart = () => {
    setParts([...parts, { partId: '', quantity: 1 }]);
  };

  const handleRemovePart = (index) => {
    if (parts.length > 1) {
      const newParts = parts.filter((_, i) => i !== index);
      setParts(newParts);
    }
  };

  const handlePartChange = (index, field, value) => {
    const newParts = [...parts];
    newParts[index][field] = value;
    setParts(newParts);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      const endpoint = controlOrderType === 'PRODUCTION' 
        ? `/production-control-orders/${controlOrder.id}/request-parts`
        : `/assembly-control-orders/${controlOrder.id}/request-parts`;
      
      const payload = {
        requiredParts: parts.map(p => ({
          partId: parseInt(p.partId),
          quantityRequested: parseInt(p.quantity),
          unit: 'piece'
        })),
        neededBy: neededBy || null,
        notes: notes
      };

      const response = await fetch(`/api${endpoint}`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        },
        body: JSON.stringify(payload)
      });

      if (!response.ok) {
        throw new Error('Failed to request parts');
      }

      onSuccess?.();
      onClose();
      // Reset form
      setParts([{ partId: '', quantity: 1 }]);
      setNeededBy('');
      setNotes('');
    } catch (err) {
      setError(err.message || 'Failed to request parts');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h2>Request Parts from Warehouse</h2>
          <button className="modal-close" onClick={onClose}>&times;</button>
        </div>

        <form onSubmit={handleSubmit}>
          <div className="modal-body">
            <div className="form-info">
              <p><strong>Order:</strong> {controlOrder?.controlOrderNumber || controlOrder?.orderNumber}</p>
              <p><strong>Type:</strong> {controlOrderType}</p>
            </div>

            {error && (
              <div className="alert alert-danger">{error}</div>
            )}

            <div className="parts-list">
              <h3>Required Parts</h3>
              {parts.map((part, index) => (
                <div key={index} className="part-row">
                  <div className="form-group">
                    <label>Part ID</label>
                    <input
                      type="number"
                      className="form-control"
                      value={part.partId}
                      onChange={(e) => handlePartChange(index, 'partId', e.target.value)}
                      required
                      min="1"
                    />
                  </div>
                  <div className="form-group">
                    <label>Quantity</label>
                    <input
                      type="number"
                      className="form-control"
                      value={part.quantity}
                      onChange={(e) => handlePartChange(index, 'quantity', e.target.value)}
                      required
                      min="1"
                    />
                  </div>
                  {parts.length > 1 && (
                    <Button
                      type="button"
                      variant="danger"
                      size="small"
                      onClick={() => handleRemovePart(index)}
                    >
                      Remove
                    </Button>
                  )}
                </div>
              ))}
              
              <Button 
                type="button"
                variant="outline" 
                size="small" 
                onClick={handleAddPart}
              >
                + Add Another Part
              </Button>
            </div>
            
            <div className="form-group">
              <label>Needed By (optional)</label>
              <input
                type="datetime-local"
                className="form-control"
                value={neededBy}
                onChange={(e) => setNeededBy(e.target.value)}
              />
            </div>
            
            <div className="form-group">
              <label>Notes</label>
              <textarea
                className="form-control"
                rows="3"
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                placeholder="Special instructions or notes..."
              />
            </div>
          </div>

          <div className="modal-footer">
            <Button variant="secondary" onClick={onClose} disabled={loading}>
              Cancel
            </Button>
            <Button variant="primary" type="submit" disabled={loading}>
              {loading ? 'Submitting...' : 'Request Parts'}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}

RequestPartsModal.propTypes = {
  show: PropTypes.bool.isRequired,
  onClose: PropTypes.func.isRequired,
  controlOrder: PropTypes.object,
  controlOrderType: PropTypes.oneOf(['PRODUCTION', 'ASSEMBLY']).isRequired,
  onSuccess: PropTypes.func
};

export default RequestPartsModal;
