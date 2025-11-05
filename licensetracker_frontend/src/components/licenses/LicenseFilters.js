import React from 'react';
import { Card, Row, Col, Form, Button } from 'react-bootstrap';

const LicenseFilters = ({ filters, setFilters, pageSize, setPageSize, setCurrentPage }) => {
  const handleFilterChange = (e) => {
    setFilters({
      ...filters,
      [e.target.name]: e.target.value
    });
  };

  const resetFilters = () => {
    setFilters({
      search: '',
      region: '',
      licenseType: '',
      active: ''
    });
  };

  return (
    <Card className="shadow-sm border-0 mb-3">
      <Card.Header className="bg-white border-bottom">
        <h5 className="mb-0">
          <i className="bi bi-funnel me-2"></i>Filters
        </h5>
      </Card.Header>
      <Card.Body>
        <Row className="gy-3">
          <Col xs={12} md={3}>
            <Form.Group>
              <Form.Label>Search</Form.Label>
              <Form.Control
                type="text"
                name="search"
                placeholder="License key or software..."
                value={filters.search}
                onChange={handleFilterChange}
              />
            </Form.Group>
          </Col>
          <Col xs={6} md={2}>
            <Form.Group>
              <Form.Label>Region</Form.Label>
              <Form.Select name="region" value={filters.region} onChange={handleFilterChange}>
                <option value="">All Regions</option>
                <option value="BANGALORE">BANGALORE</option>
                <option value="CHENNAI">CHENNAI</option>
                <option value="DELHI">DELHI</option>
                <option value="MUMBAI">MUMBAI</option>
                <option value="HYDERABAD">HYDERABAD</option>
                <option value="KOLKATA">KOLKATA</option>
              </Form.Select>
            </Form.Group>
          </Col>
          <Col xs={6} md={2}>
            <Form.Group>
              <Form.Label>Type</Form.Label>
              <Form.Select name="licenseType" value={filters.licenseType} onChange={handleFilterChange}>
                <option value="">All Types</option>
                <option value="PER_DEVICE">Per Device</option>
                <option value="PER_USER">Per User</option>
                <option value="ENTERPRISE">Enterprise</option>
                <option value="REGION">Region</option>
              </Form.Select>
            </Form.Group>
          </Col>
          <Col xs={6} md={2}>
            <Form.Group>
              <Form.Label>Status</Form.Label>
              <Form.Select name="active" value={filters.active} onChange={handleFilterChange}>
                <option value="">All Status</option>
                <option value="true">Active</option>
                <option value="false">Inactive</option>
              </Form.Select>
            </Form.Group>
          </Col>
          <Col xs={6} md={2}>
            <Form.Group>
              <Form.Label>Per Page</Form.Label>
              <Form.Select
                value={pageSize}
                onChange={(e) => {
                  setPageSize(Number(e.target.value));
                  setCurrentPage(0);
                }}
              >
                <option value="5">5</option>
                <option value="10">10</option>
                <option value="25">25</option>
                <option value="50">50</option>
              </Form.Select>
            </Form.Group>
          </Col>
          <Col xs={12} md={2} className="d-flex align-items-end">
            <Button variant="outline-secondary" onClick={resetFilters} className="w-100">
              Reset
            </Button>
          </Col>
        </Row>
      </Card.Body>
    </Card>
  );
};

export default LicenseFilters;
