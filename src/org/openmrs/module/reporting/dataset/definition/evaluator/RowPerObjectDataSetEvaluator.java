/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.reporting.dataset.definition.evaluator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.context.Context;
import org.openmrs.module.reporting.common.ObjectUtil;
import org.openmrs.module.reporting.dataset.DataSet;
import org.openmrs.module.reporting.dataset.DataSetColumn;
import org.openmrs.module.reporting.dataset.RowPerObjectDataSet;
import org.openmrs.module.reporting.dataset.column.EvaluatedColumnDefinition;
import org.openmrs.module.reporting.dataset.column.definition.ColumnDefinition;
import org.openmrs.module.reporting.dataset.column.service.DataSetColumnDefinitionService;
import org.openmrs.module.reporting.dataset.definition.DataSetDefinition;
import org.openmrs.module.reporting.dataset.definition.RowPerObjectDataSetDefinition;
import org.openmrs.module.reporting.evaluation.EvaluationContext;
import org.openmrs.module.reporting.evaluation.EvaluationException;
import org.openmrs.module.reporting.evaluation.parameter.Mapped;
import org.openmrs.module.reporting.query.QueryResult;

/**
 * The logic that evaluates a {@link RowPerObjectDataSetDefinition} and produces an {@link DataSet}
 */
public abstract class RowPerObjectDataSetEvaluator implements DataSetEvaluator {

	protected Log log = LogFactory.getLog(this.getClass());

	/**
	 * Public constructor
	 */
	public RowPerObjectDataSetEvaluator() { }
	
	/**
	 * Implementations of this method should evaluate the appropriate id filters in the DataSetDefinition and
	 * populate these QueryResults within the Context
	 */
	public abstract void populateFilterQueryResults(RowPerObjectDataSetDefinition<?> dsd, EvaluationContext context) throws EvaluationException;
	
	/**
	 * Implementations of this method should return the base QueryResult that is appropriate for the passed DataSetDefinition
	 */
	public abstract QueryResult getBaseQueryResult(RowPerObjectDataSetDefinition<?> dsd, EvaluationContext context);
	
	/**
	 * @see DataSetEvaluator#evaluate(DataSetDefinition, EvaluationContext)
	 */
	@SuppressWarnings("unchecked")
	public DataSet evaluate(DataSetDefinition dataSetDefinition, EvaluationContext context) throws EvaluationException {
		
		RowPerObjectDataSetDefinition<? extends ColumnDefinition> dsd = (RowPerObjectDataSetDefinition<? extends ColumnDefinition>) dataSetDefinition;
		DataSetColumnDefinitionService service = Context.getService(DataSetColumnDefinitionService.class);
		
		context = ObjectUtil.nvl(context, new EvaluationContext());
		populateFilterQueryResults(dsd, context);
		QueryResult baseQuery = getBaseQueryResult(dsd, context);
		
		RowPerObjectDataSet dataSet = new RowPerObjectDataSet(dsd, context);

		for (Mapped<? extends ColumnDefinition> mappedDef : dsd.getColumnDefinitions()) {
			
			EvaluatedColumnDefinition evaluatedColumnDef = service.evaluate(mappedDef, context);
			ColumnDefinition cd = evaluatedColumnDef.getDefinition();
			DataSetColumn column = new DataSetColumn(cd.getName(), cd.getName(), cd.getDataType()); // TODO: Support One-Many column definition to column
			
			for (Integer id : baseQuery.getMemberIds()) {
				dataSet.addColumnValue(id, column, evaluatedColumnDef.getColumnValues().get(id));
			}
		}

		return dataSet;
	}
}
