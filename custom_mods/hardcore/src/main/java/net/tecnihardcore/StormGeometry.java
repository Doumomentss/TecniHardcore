package net.tecnihardcore;

import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.*;

/** Bounded meshes; the funnel and dangerous silhouettes survive minimal particle settings. */
final class StormGeometry {
    static void clouds(MatrixStack m,VertexConsumer v,Vec3d p,double time,float opacity,int radius){double size=Math.min(140,radius*.7);for(int layer=0;layer<2;layer++)for(int i=0;i<9;i++){double a=i*Math.PI*2/9+time*.001*(layer==0?1:-.7),distance=size*(layer==0?.55:.8);sphere(m,v,p.add(Math.cos(a)*distance,60+layer*13+Math.sin(a*2)*5,Math.sin(a)*distance),size*.6,12+layer*3,size*.48,12,4,opacity*(layer==0?.78F:.48F),.17F+layer*.09F,.19F+layer*.09F,.23F+layer*.09F,i*.4);}}
    static void quake(MatrixStack m,VertexConsumer v,VertexConsumer lines,Vec3d center,double pulse,float opacity,int radius){if(pulse<40||pulse>120)return;float fade=(float)(120-pulse)/80;for(int arm=0;arm<10;arm++){double a=arm*Math.PI/5;Vec3d last=center.add(0,.03,0);for(int step=1;step<=7;step++){double d=step*Math.min(radius/7.0,24),wiggle=Math.sin(arm*3+step*2.2)*3;Vec3d next=center.add(Math.cos(a)*d+Math.sin(a)*wiggle,.045,Math.sin(a)*d-Math.cos(a)*wiggle);beam(m,lines,last,next,.18,.46F,.35F,.22F,opacity*fade);if(step%2==0)sphere(m,v,next.add(0,.8+Math.sin(pulse*.1+step)*.5,0),2.4,1.6,1.8,6,3,opacity*fade*.55F,.48F,.4F,.31F,a);last=next;}}}
    static void tornado(MatrixStack m,VertexConsumer v,VertexConsumer lines,Vec3d p,double time,float opacity,boolean distant,int width,double height){
        double scale=width/120.0;int segments=distant?20:32,bands=distant?12:20;
        for(int layer=0;layer<2;layer++)for(int y=0;y<bands;y++){
            double f=y/(double)bands,g=(y+1)/(double)bands;
            double r0=ExpansionRules.tornadoRadius(f)*scale*(1-layer*.11),r1=ExpansionRules.tornadoRadius(g)*scale*(1-layer*.11);
            for(int i=0;i<segments;i++){
                double a=i*Math.PI*2/segments+time*.018+f*5+layer,b=(i+1)*Math.PI*2/segments+time*.018+f*5+layer;
                quad(m,v,p.add(Math.cos(a)*r0,f*height,Math.sin(a)*r0),p.add(Math.cos(b)*r0,f*height,Math.sin(b)*r0),p.add(Math.cos(b+.2)*r1,g*height,Math.sin(b+.2)*r1),p.add(Math.cos(a+.2)*r1,g*height,Math.sin(a+.2)*r1),i/(float)segments,(i+1)/(float)segments,(float)(f*4+time*.002),(float)(g*4+time*.002),opacity*(layer==0?.96F:.82F),.32F,.35F,.39F);
            }
        }
        for(int i=0;i<(distant?4:8);i++){double a=i*Math.PI/4+time*.003;sphere(m,v,p.add(Math.cos(a)*29*scale,height*.925+Math.sin(a*2)*2,Math.sin(a)*29*scale),29*scale,14,29*scale,distant?10:14,4,opacity*.7F,.3F,.34F,.38F,i*.3);}
        // Dust skirt and floating rubble are cosmetic geometry, never real blocks.
        sphere(m,v,p.add(0,2,0),27*scale,4,27*scale,20,3,opacity*.5F,.6F,.49F,.3F,time*.004);
        for(int i=0;i<(distant?5:16);i++){double a=time*.033+i*2.4,y=9+(i*5+time*.16)%80,r=7+y*.14;sphere(m,v,p.add(Math.cos(a)*r,y,Math.sin(a)*r),.45+i%3*.25,.5,.5,6,3,opacity,.5F,.45F,.4F,i);}
        if((int)time%80<7){Vec3d origin=p.add(0,30+((int)time/80%3)*24,0);lightning(m,lines,origin,35,((int)time/80)*17,opacity*.8F);}
    }

    static void debris(MatrixStack m,VertexConsumerProvider vertices,Vec3d p,double time,int width,double funnelHeight,boolean distant,net.minecraft.client.option.ParticlesMode particles,int destruction,java.util.UUID id){
        int count=WeatherRules.debrisCount(destruction,distant,particles==net.minecraft.client.option.ParticlesMode.MINIMAL?2:particles==net.minecraft.client.option.ParticlesMode.DECREASED?1:0);
        var states=new net.minecraft.block.BlockState[]{net.minecraft.block.Blocks.DIRT.getDefaultState(),net.minecraft.block.Blocks.STONE.getDefaultState(),net.minecraft.block.Blocks.OAK_PLANKS.getDefaultState(),net.minecraft.block.Blocks.OAK_LEAVES.getDefaultState(),net.minecraft.block.Blocks.COBBLESTONE.getDefaultState()};
        states=StormRubbleVisuals.palette(id,states);
        var renderer=net.minecraft.client.MinecraftClient.getInstance().getBlockRenderManager();
        for(int i=0;i<count;i++){double height=4+(i*17+time*(.27+destruction*.09))%(funnelHeight*.85),angle=time*(.022+destruction*.003+(i%5)*.003)+i*2.3999,radius=ExpansionRules.tornadoRadius(height/funnelHeight)*width/120.0*(1.12+(i%7)*.07);
            m.push();m.translate(p.x+Math.cos(angle)*radius,p.y+height,p.z+Math.sin(angle)*radius);m.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotation((float)(time*.035+i)));m.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotation((float)(time*.023+i*.7)));float size=.5F+(i%5)*.24F;m.scale(size,size,size);m.translate(-.5,-.5,-.5);renderer.renderBlockAsEntity(states[i%states.length],m,vertices,15728880,net.minecraft.client.render.OverlayTexture.DEFAULT_UV);m.pop();
        }
    }

    static void meteor(MatrixStack m,VertexConsumer v,VertexConsumer lines,Vec3d target,double cycle,float opacity){
        if(cycle<=60){double remaining=Math.max(0,60-cycle);Vec3d p=target.add(remaining*.22,remaining*.8,remaining*.12);
            sphere(m,v,p,1.8,1.6,1.8,14,8,opacity,1,.75F,.4F,cycle*.08);
            for(int i=0;i<7;i++)sphere(m,v,p.add(i*.6,i*2.1,i*.32),Math.max(.15,1.5-i*.18),2,Math.max(.15,1.5-i*.18),8,3,opacity*(.55F-i*.065F),1,.28F+i*.05F,.05F,cycle*.1+i);
        }else if(cycle<78){float fade=(float)(78-cycle)/18;sphere(m,v,target.add(0,.08,0),4,.08,4,20,3,opacity*fade*.6F,.34F,.16F,.08F,0);for(int i=0;i<3;i++)ring(m,lines,target.add(0,.12,0),4+(cycle-60)*.25+i*.35,1,.55F,.16F,fade);}
    }

    static void lightning(MatrixStack m,VertexConsumer lines,Vec3d target,double height,long seed,float opacity){
        var random=new java.util.Random(seed);Vec3d previous=target.add(0,height,0);
        for(int i=1;i<=9;i++){Vec3d next=target.add(i==9?0:random.nextDouble()*5-2.5,height*(1-i/9.0),i==9?0:random.nextDouble()*5-2.5);beam(m,lines,previous,next,.09,.84F,.9F,1,opacity);if(i==3||i==5){Vec3d branch=next.add(random.nextBoolean()?7:-7,-height*.18,random.nextDouble()*6-3);beam(m,lines,next,branch,.045,.64F,.75F,1,opacity*.7F);}previous=next;}
    }
    static void beam(MatrixStack m,VertexConsumer lines,Vec3d a,Vec3d b,double width,float r,float g,float blue,float alpha){Vec3d n=b.subtract(a).normalize();for(double offset:new double[]{-width,0,width}){lines.vertex(m.peek().getPositionMatrix(),(float)(a.x+offset),(float)a.y,(float)a.z).color(r,g,blue,alpha).normal(m.peek().getNormalMatrix(),(float)n.x,(float)n.y,(float)n.z).next();lines.vertex(m.peek().getPositionMatrix(),(float)(b.x+offset),(float)b.y,(float)b.z).color(r,g,blue,alpha).normal(m.peek().getNormalMatrix(),(float)n.x,(float)n.y,(float)n.z).next();}}
    static void ring(MatrixStack m,VertexConsumer lines,Vec3d p,double radius,float r,float g,float b,float a){for(int i=0;i<48;i++){double angle=i*Math.PI/24,next=(i+1)*Math.PI/24;beam(m,lines,p.add(Math.cos(angle)*radius,.15,Math.sin(angle)*radius),p.add(Math.cos(next)*radius,.15,Math.sin(next)*radius),.055,r,g,b,a);}}
    private static final java.util.Map<Integer,double[][]> sphereMeshes=new java.util.HashMap<>();
    private static double[][] sphereMesh(int segments,int bands){return sphereMeshes.computeIfAbsent(segments*100+bands,key->{double[][] points=new double[segments*bands*4][5];int n=0;for(int j=0;j<bands;j++)for(int i=0;i<segments;i++)for(int corner=0;corner<4;corner++){int x=i+(corner==1||corner==2?1:0),y=j+(corner>=2?1:0);double a=-Math.PI/2+y*Math.PI/bands,u=x*Math.PI*2/segments,wobble=1+.045*Math.sin(u*5+a*8);points[n++]=new double[]{Math.cos(a)*Math.cos(u)*wobble,Math.sin(a),Math.cos(a)*Math.sin(u)*wobble,x/(double)segments,y/(double)bands};}return points;});}
    private static void sphere(MatrixStack m,VertexConsumer v,Vec3d p,double rx,double ry,double rz,int segments,int bands,float alpha,float r,float g,float b,double rotation){double cosine=Math.cos(rotation),sine=Math.sin(rotation);var matrix=m.peek().getPositionMatrix();for(var unit:sphereMesh(segments,bands)){float x=(float)(p.x+(unit[0]*cosine-unit[2]*sine)*rx),y=(float)(p.y+unit[1]*ry),z=(float)(p.z+(unit[0]*sine+unit[2]*cosine)*rz);v.vertex(matrix,x,y,z).color(r,g,b,alpha).texture((float)unit[3],(float)unit[4]).next();}}
    private static Vec3d point(Vec3d p,double x,double y,double z,double a,double u){double n=1+.045*Math.sin(u*5+a*8);return p.add(Math.cos(a)*Math.cos(u)*x*n,Math.sin(a)*y,Math.cos(a)*Math.sin(u)*z*n);}
    private static void quad(MatrixStack m,VertexConsumer v,Vec3d a,Vec3d b,Vec3d c,Vec3d d,float u,float u1,float y,float y1,float alpha,float r,float g,float blue){vertex(m,v,a,u,y,alpha,r,g,blue);vertex(m,v,b,u1,y,alpha,r,g,blue);vertex(m,v,c,u1,y1,alpha,r,g,blue);vertex(m,v,d,u,y1,alpha,r,g,blue);}
    private static void vertex(MatrixStack m,VertexConsumer v,Vec3d p,float u,float y,float a,float r,float g,float b){v.vertex(m.peek().getPositionMatrix(),(float)p.x,(float)p.y,(float)p.z).color(r,g,b,a).texture(u,y).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(m.peek().getNormalMatrix(),0,1,0).next();}
}
