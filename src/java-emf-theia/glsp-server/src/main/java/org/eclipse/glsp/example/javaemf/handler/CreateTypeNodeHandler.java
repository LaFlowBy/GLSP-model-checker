/********************************************************************************
 * Copyright (c) 2022 EclipseSource and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the Eclipse
 * Public License v. 2.0 are satisfied:
 * -- GNU General Public License, version 2 with the GNU Classpath Exception
 * which is available at https://www.gnu.org/software/classpath/license.html
 * -- MIT License which is available at https://opensource.org/license/mit.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0 OR MIT
 ********************************************************************************/
package org.eclipse.glsp.example.javaemf.handler;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import org.eclipse.emf.common.command.Command;
import org.eclipse.emf.common.command.CompoundCommand;
import org.eclipse.emf.edit.command.AddCommand;
import org.eclipse.emf.edit.domain.EditingDomain;
import org.eclipse.glsp.example.javaemf.TypeGraphModelTypes;
import org.eclipse.glsp.example.typegraph.typegraph.ModelFactory;
import org.eclipse.glsp.example.typegraph.typegraph.ModelPackage;
import org.eclipse.glsp.example.typegraph.typegraph.TypeGraph;
import org.eclipse.glsp.example.typegraph.typegraph.TypeNode;
import org.eclipse.glsp.graph.GModelElement;
import org.eclipse.glsp.graph.GPoint;
import org.eclipse.glsp.graph.GraphPackage;
import org.eclipse.glsp.graph.util.GraphUtil;
import org.eclipse.glsp.server.emf.EMFCreateOperationHandler;
import org.eclipse.glsp.server.emf.EMFIdGenerator;
import org.eclipse.glsp.server.emf.model.notation.Diagram;
import org.eclipse.glsp.server.emf.model.notation.NotationFactory;
import org.eclipse.glsp.server.emf.model.notation.NotationPackage;
import org.eclipse.glsp.server.emf.model.notation.SemanticElementReference;
import org.eclipse.glsp.server.emf.model.notation.Shape;
import org.eclipse.glsp.server.emf.notation.EMFNotationModelState;
import org.eclipse.glsp.server.operations.CreateNodeOperation;
import org.eclipse.glsp.server.utils.LayoutUtil;

import com.google.inject.Inject;

public class CreateTypeNodeHandler extends EMFCreateOperationHandler<CreateNodeOperation> {

    @Inject
    protected EMFNotationModelState modelState;

    @Inject
    protected EMFIdGenerator idGenerator;

    public CreateTypeNodeHandler() {
        super(TypeGraphModelTypes.TYPE_NODE);
    }

    @Override
    public Optional<Command> createCommand(final CreateNodeOperation operation) {
        GModelElement container = modelState.getIndex().get(operation.getContainerId()).orElseGet(modelState::getRoot);
        Optional<GPoint> absoluteLocation = operation.getLocation();
        Optional<GPoint> relativeLocation = absoluteLocation
            .map(location -> LayoutUtil.getRelativeLocation(location, container));

        return Optional.of(createTypeNodeAndShape(relativeLocation));
    }

    @Override
    public String getLabel() { return "TypeNode"; }

    protected Command createTypeNodeAndShape(final Optional<GPoint> relativeLocation) {
        TypeGraph typeGraph = modelState.getSemanticModel(TypeGraph.class).orElseThrow();
        Diagram diagram = modelState.getNotationModel();
        EditingDomain editingDomain = modelState.getEditingDomain();

        TypeNode newTypeNode = createTypeNode();
        Command TypeNodeCommand = AddCommand.create(editingDomain, typeGraph,
            ModelPackage.Literals.TYPE_GRAPH__TYPENODES, newTypeNode);

        Shape shape = createShape(idGenerator.getOrCreateId(newTypeNode), relativeLocation);
        Command shapeCommand = AddCommand.create(editingDomain, diagram,
            NotationPackage.Literals.DIAGRAM__ELEMENTS, shape);

        CompoundCommand compoundCommand = new CompoundCommand();
        compoundCommand.append(TypeNodeCommand);
        compoundCommand.append(shapeCommand);
        return compoundCommand;
    }

    protected TypeNode createTypeNode() {
        TypeNode newTypeNode = ModelFactory.eINSTANCE.createTypeNode();
        newTypeNode.setId(UUID.randomUUID().toString());
        setInitialName(newTypeNode);
        return newTypeNode;
    }

    protected void setInitialName(final TypeNode typeNode) {
        Function<Integer, String> nameProvider = i -> "New" + typeNode.eClass().getName() + i;
        int nodeCounter = modelState.getIndex().getCounter(GraphPackage.Literals.GNODE, nameProvider);
        typeNode.setName(nameProvider.apply(nodeCounter));
    }

    protected Shape createShape(final String elementId, final Optional<GPoint> relativeLocation) {
        Shape newTypeNode = NotationFactory.eINSTANCE.createShape();
        newTypeNode.setPosition(relativeLocation.orElse(GraphUtil.point(0, 0)));
        newTypeNode.setSize(GraphUtil.dimension(60, 25));
        SemanticElementReference reference = NotationFactory.eINSTANCE.createSemanticElementReference();
        reference.setElementId(elementId);
        newTypeNode.setSemanticElement(reference);
        return newTypeNode;
    }
}
