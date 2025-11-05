import React from 'react';
import { Table, Pagination, Badge } from 'react-bootstrap';
import LicenseRow from './LicenseRow';

const LicenseTable = ({ licenses, currentPage, totalPages, onPageChange, onDelete, navigate }) => {
  return (
    <div class="overflow-auto" style={{width : "75vw"}}>
      <Table hover responsive>
        <thead className="table-light">
          <tr>
            <th>License Key</th>
            <th>Software</th>
            <th>Type</th>
            <th>Vendor</th>
            <th>Usage</th>
            <th>Valid From</th>
            <th>Valid To</th>
            <th>Region</th>
            <th>Cost</th>
            <th>Status</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {licenses.length > 0 ? (
            licenses.map((license) => (
              <LicenseRow
                key={license.id}
                license={license}
                onDelete={onDelete}
                navigate={navigate}
              />
            ))
          ) : (
            <tr>
              <td colSpan="11" className="text-center py-4">
                <i className="bi bi-inbox text-muted" style={{ fontSize: '3rem' }}></i>
                <p className="text-muted mt-2">No licenses found</p>
              </td>
            </tr>
          )}
        </tbody>
      </Table>

      {totalPages > 1 && (
        <div className="d-flex justify-content-center mt-4">
          <Pagination>
            <Pagination.First
              onClick={() => onPageChange(0)}
              disabled={currentPage === 0}
            />
            <Pagination.Prev
              onClick={() => onPageChange(currentPage - 1)}
              disabled={currentPage === 0}
            />

            {[...Array(totalPages)].map((_, index) => (
              <Pagination.Item
                key={index}
                active={index === currentPage}
                onClick={() => onPageChange(index)}
              >
                {index + 1}
              </Pagination.Item>
            ))}

            <Pagination.Next
              onClick={() => onPageChange(currentPage + 1)}
              disabled={currentPage === totalPages - 1}
            />
            <Pagination.Last
              onClick={() => onPageChange(totalPages - 1)}
              disabled={currentPage === totalPages - 1}
            />
          </Pagination>
        </div>
      )}
    </div>
  );
};

export default LicenseTable;
