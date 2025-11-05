import React, { useState } from 'react';
import { Modal, Button, Form, Alert, Badge, Spinner, ProgressBar } from 'react-bootstrap';
import deviceService from '../../services/deviceService';

const BulkUploadModel = ({ show, onHide, onUploadComplete }) => {
    const [file, setFile] = useState(null);
    const [uploading, setUploading] = useState(false);
    const [result, setResult] = useState(null);
    const [error, setError] = useState('');

    const handleFileChange = (e) => {
        const selectedFile = e.target.files[0];
        if (selectedFile) {
            if (!selectedFile.name.endsWith('.csv')) {
                setError('Please select a CSV file');
                setFile(null);
                return;
            }
            setFile(selectedFile);
            setError('');
            setResult(null);
        }
    };

    const handleDownloadTemplate = async () => {
        try {
            const blob = await deviceService.downloadTemplate();
            const url = window.URL.createObjectURL(blob);
            const link = document.createElement('a');
            link.href = url;
            link.download = 'device_upload_template.csv';
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
            window.URL.revokeObjectURL(url);
        } catch (error) {
            console.error('Error downloading template:', error);
            setError('Failed to download template');
        }
    };

    const handleUpload = async () => {
        if (!file) {
            setError('Please select a file');
            return;
        }

        setUploading(true);
        setError('');
        setResult(null);

        try {
            const uploadResult = await deviceService.bulkUpload(file);
            setResult(uploadResult);
            
            if (uploadResult.successCount > 0) {
                setTimeout(() => {
                    onUploadComplete();
                }, 2000);
            }
        } catch (error) {
            console.error('Upload error:', error);
            setError(error.response?.data?.message || 'Failed to upload file');
        } finally {
            setUploading(false);
        }
    };

    const handleClose = () => {
        setFile(null);
        setResult(null);
        setError('');
        onHide();
    };

    const getSuccessPercentage = () => {
        if (!result || result.totalRecords === 0) return 0;
        return (result.successCount / result.totalRecords) * 100;
    };

    return (
        <Modal show={show} onHide={handleClose} size="lg">
            <Modal.Header closeButton>
                <Modal.Title>
                    <i className="bi bi-upload me-2"></i>
                    Bulk Upload Devices
                </Modal.Title>
            </Modal.Header>
            <Modal.Body>
                {error && (
                    <Alert variant="danger" dismissible onClose={() => setError('')}>
                        <i className="bi bi-exclamation-triangle me-2"></i>
                        {error}
                    </Alert>
                )}

                {!result && (
                    <>
                        <Alert variant="info">
                            <h6 className="alert-heading">
                                <i className="bi bi-info-circle me-2"></i>
                                Instructions
                            </h6>
                            <ol className="mb-0">
                                <li>Download the CSV template</li>
                                <li>Fill in your device data</li>
                                <li>Upload the completed CSV file</li>
                            </ol>
                        </Alert>

                        <div className="mb-3">
                            <Button 
                                variant="outline-primary" 
                                onClick={handleDownloadTemplate}
                                className="w-100"
                            >
                                <i className="bi bi-download me-2"></i>
                                Download CSV Template
                            </Button>
                        </div>

                        <Form.Group className="mb-3">
                            <Form.Label>Select CSV File</Form.Label>
                            <Form.Control
                                type="file"
                                accept=".csv"
                                onChange={handleFileChange}
                                disabled={uploading}
                            />
                            <Form.Text className="text-muted">
                                Only CSV files are supported. Maximum file size: 10MB
                            </Form.Text>
                        </Form.Group>

                        {file && (
                            <Alert variant="success">
                                <i className="bi bi-file-earmark-check me-2"></i>
                                Selected file: <strong>{file.name}</strong> ({(file.size / 1024).toFixed(2)} KB)
                            </Alert>
                        )}
                    </>
                )}

                {result && (
                    <div>
                        <h5 className="mb-3">Upload Results</h5>
                        
                        <div className="mb-3">
                            <div className="d-flex justify-content-between mb-2">
                                <span>Success Rate</span>
                                <span>
                                    <Badge bg="success">{result.successCount}</Badge>
                                    {' / '}
                                    <Badge bg="secondary">{result.totalRecords}</Badge>
                                </span>
                            </div>
                            <ProgressBar>
                                <ProgressBar 
                                    variant="success" 
                                    now={getSuccessPercentage()} 
                                    label={`${result.successCount} success`}
                                />
                                <ProgressBar 
                                    variant="danger" 
                                    now={(result.failureCount / result.totalRecords) * 100} 
                                    label={`${result.failureCount} failed`}
                                />
                            </ProgressBar>
                        </div>

                        {result.successMessages.length > 0 && (
                            <Alert variant="success">
                                <h6 className="alert-heading">
                                    <i className="bi bi-check-circle me-2"></i>
                                    Successfully Uploaded ({result.successMessages.length})
                                </h6>
                                <div style={{ maxHeight: '150px', overflowY: 'auto' }}>
                                    <ul className="mb-0">
                                        {result.successMessages.slice(0, 10).map((msg, idx) => (
                                            <li key={idx}><small>{msg}</small></li>
                                        ))}
                                        {result.successMessages.length > 10 && (
                                            <li><small>... and {result.successMessages.length - 10} more</small></li>
                                        )}
                                    </ul>
                                </div>
                            </Alert>
                        )}

                        {result.errorMessages.length > 0 && (
                            <Alert variant="danger">
                                <h6 className="alert-heading">
                                    <i className="bi bi-x-circle me-2"></i>
                                    Errors ({result.errorMessages.length})
                                </h6>
                                <div style={{ maxHeight: '200px', overflowY: 'auto' }}>
                                    <ul className="mb-0">
                                        {result.errorMessages.map((msg, idx) => (
                                            <li key={idx}><small>{msg}</small></li>
                                        ))}
                                    </ul>
                                </div>
                            </Alert>
                        )}
                    </div>
                )}
            </Modal.Body>
            <Modal.Footer>
                <Button variant="secondary" onClick={handleClose}>
                    Close
                </Button>
                {!result && (
                    <Button 
                        variant="primary" 
                        onClick={handleUpload}
                        disabled={!file || uploading}
                    >
                        {uploading ? (
                            <>
                                <Spinner size="sm" className="me-2" />
                                Uploading...
                            </>
                        ) : (
                            <>
                                <i className="bi bi-cloud-upload me-2"></i>
                                Upload Devices
                            </>
                        )}
                    </Button>
                )}
            </Modal.Footer>
        </Modal>
    );
};

export default BulkUploadModel;
